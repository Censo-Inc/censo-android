package co.censo.censo.presentation.beneficiary_owner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * After completing actions we will receive a new owner state
 *
 * That owner state will have a beneficiary data model that drives our UI
 *
 * Beneficiary Null: User needs to add label and call API to invite beneficiary
 *
 * Beneficiary Status:
 *
 * Initial -> Waiting for beneficiary to accept invite. Share invite via deeplink
 *
 * Accepted -> Beneficiary has accepted invite, generate TOTP codes
 *
 * Verification Submitted -> Beneficiary has submitted code, check it and confirm beneficiary
 *
 *      To confirm beneficiary we will need to encrypt the key that we retrieve from Google Drive
 *
 * Activated -> We are all set
 *
 */

@HiltViewModel
class BeneficiaryOwnerViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer,
    private val totpGenerator: TotpGenerator,
) : ViewModel() {

    var state by mutableStateOf(BeneficiaryOwnerState())
        private set

    fun onStart() {
        viewModelScope.launch {
            val ownerState = ownerRepository.getOwnerStateValue()
            onOwnerState(ownerState, updateUIState = true)
        }
    }

    fun onResume() {
        if (state.ownerState?.policy?.beneficiary?.status?.shouldGenerateBeneficiaryTotp() == true) {
            startTimers(state.ownerState?.policy?.beneficiary?.status!!)
        }
    }

    fun onPause() {
        stopTimers()
    }

    fun retrieveOwnerState(silently: Boolean = false) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading)
        }

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()
            if (!silently) {
                state = state.copy(
                    userResponse = response
                )
            }

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }
        }
    }

    private fun onOwnerState(
        ownerState: OwnerState,
        updateGlobalState: Boolean = true,
        updateUIState: Boolean = false
    ) {
        if (ownerState is OwnerState.Ready) {
            state = state.copy(ownerState = ownerState)
            if (updateUIState) {
                ownerState.policy.beneficiary?.status?.let {
                    state = state.copy(beneficiaryOwnerUIState = it.toBeneficiaryOwnerUIState())
                }
            }
            checkIfNeedToVerifyBeneficiary(ownerState)
        }

        if (updateGlobalState) {
            ownerRepository.updateOwnerState(ownerState)
        }
    }

    fun updateBeneficiaryLabel(updatedLabel: String) {
        state = state.copy(beneficiaryLabel = updatedLabel)
    }

    fun inviteBeneficiary() {
        state = state.copy(inviteBeneficiaryResource = Resource.Loading)

        try {
            viewModelScope.launch(Dispatchers.IO) {
                val totpSecret = totpGenerator.generateSecret()
                val encryptedTotpSecret =
                    keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

                val inviteBeneficiaryResponse = ownerRepository.inviteBeneficiary(
                    label = state.beneficiaryLabel,
                    deviceEncryptedTotpSecret = encryptedTotpSecret
                )

                state = if (inviteBeneficiaryResponse is Resource.Success) {

                    onOwnerState(
                        inviteBeneficiaryResponse.data.ownerState,
                        updateUIState = true
                    )

                    state.copy(
                        inviteBeneficiaryResource = inviteBeneficiaryResponse,
                        ownerState = inviteBeneficiaryResponse.data.ownerState as? OwnerState.Ready
                    )
                } else {
                    state.copy(inviteBeneficiaryResource = inviteBeneficiaryResponse)
                }
            }
        } catch (e: Exception) {
            state = state.copy(inviteBeneficiaryResource = Resource.Error(e))
        }
    }

    private fun nextTotpTimerTick(status: BeneficiaryStatus) {
        val now = Clock.System.now()
        val updatedCounter = now.epochSeconds.div(TotpGenerator.Companion.CODE_EXPIRATION)
        val secondsLeft = now.epochSeconds - (updatedCounter.times(TotpGenerator.CODE_EXPIRATION))

        state = if (state.counter != updatedCounter) {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
                counter = updatedCounter,
                beneficiaryCode = generateTimeCode(status)
            )
        } else {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
            )
        }
    }

    private fun generateTimeCode(beneficiaryStatus: BeneficiaryStatus): String {
        val encryptedTotpSecret = beneficiaryStatus.resolveDeviceEncryptedTotpSecret()!!

        return totpGenerator.generateCode(
            secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
            counter = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
        )
    }

    private fun checkIfNeedToVerifyBeneficiary(ownerState: OwnerState.Ready) {
        if (ownerState.policy.beneficiary?.status is BeneficiaryStatus.VerificationSubmitted) {
            verifyBeneficiary(
                ownerState.policy.beneficiary?.status as BeneficiaryStatus.VerificationSubmitted
            )
        }
    }

    private fun verifyBeneficiary(
        beneficiaryStatus: BeneficiaryStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            encryptedTotpSecret = beneficiaryStatus.deviceEncryptedTotpSecret,
            transportKey = beneficiaryStatus.beneficiaryPublicKey,
            signature = beneficiaryStatus.signature,
            timeMillis = beneficiaryStatus.timeMillis
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (codeVerified) {

                //Confirm Beneficiary Response
                val keyConfirmationTimeMillis = Clock.System.now().toEpochMilliseconds()

                val keyConfirmationMessage =
                    beneficiaryStatus.beneficiaryPublicKey.getBytes() + keyConfirmationTimeMillis.toString()
                        .toByteArray()
                val keyConfirmationSignature =
                    keyRepository.retrieveInternalDeviceKey().sign(keyConfirmationMessage)

                val partId = state.ownerState?.policy?.owner?.participantId
                val entropy = state.ownerState?.policy?.ownerEntropy

                val activateBeneficiaryResponse =
                    ownerRepository.activateBeneficiary(
                        ownerParticipantId = partId!!,
                        entropy = entropy!!,
                        beneficiaryPublicKey = beneficiaryStatus.beneficiaryPublicKey,
                        approverPublicKeys = beneficiaryStatus.approverPublicKeys,
                        keyConfirmationSignature = keyConfirmationSignature,
                        keyConfirmationTimeMillis = keyConfirmationTimeMillis
                    )

                activateBeneficiaryResponse.onSuccess {
                    onOwnerState(
                        it.ownerState,
                        updateUIState = true
                    )
                }
            } else {
                val rejectBeneficiaryVerification = ownerRepository.rejectBeneficiaryVerification()

                rejectBeneficiaryVerification.onSuccess {
                    onOwnerState(it.ownerState)
                }
            }
        }
    }

    private fun startTimers(status: BeneficiaryStatus) {
        viewModelScope.launch {
            pollingVerificationTimer.start(
                interval = CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN,
                skipFirstTick = true
            ) {
                if (state.userResponse !is Resource.Loading &&
                    state.ownerState?.policy?.beneficiary?.status !is BeneficiaryStatus.Activated
                ) {
                    retrieveOwnerState(silently = true)
                }
            }
        }

        verificationCodeTimer.start(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {
            nextTotpTimerTick(status)
        }
    }

    private fun stopTimers() {
        pollingVerificationTimer.stop()
        verificationCodeTimer.stop()
    }

    fun dismissCreateBeneficiaryError() {
        state = state.copy(createBeneficiaryError = null)
    }

    fun moveUserThroughUI(uiState: BeneficiaryOwnerUIState) {
        state = state.copy(
            beneficiaryOwnerUIState = uiState
        )
    }
}