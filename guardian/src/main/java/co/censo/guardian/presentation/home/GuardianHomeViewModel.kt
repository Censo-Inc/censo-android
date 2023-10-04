package co.censo.guardian.presentation.home

import Base58EncodedGuardianPublicKey
import Base58EncodedPrivateKey
import Base64EncodedData
import InvitationId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.SubmitGuardianVerificationApiRequest
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class GuardianHomeViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    companion object {
        const val VALID_CODE_LENGTH = 6
    }

    var state by mutableStateOf(GuardianHomeState())
        private set

    fun onStart() {
        retrieveUserState()
    }

    private fun determineGuardianUIState(
        guardianState: GuardianState?,
    ) {
        viewModelScope.launch {
            val inviteCode =
                state.invitationId.value.ifEmpty { guardianRepository.retrieveInvitationId() }
            val userSavedPrivateKey = guardianRepository.userHasKeySavedInCloud()

            val guardianUIState = if (guardianState == null) {
                if (inviteCode.isEmpty()) {
                    GuardianUIState.MISSING_INVITE_CODE
                } else {
                    GuardianUIState.INVITE_READY
                }
            } else {
                when (guardianState.phase) {
                    is GuardianPhase.WaitingForCode -> {
                        if (!userSavedPrivateKey) {
                            GuardianUIState.NEED_SAVE_KEY
                        } else {
                            GuardianUIState.WAITING_FOR_CODE
                        }
                    }

                    is GuardianPhase.WaitingForConfirmation -> GuardianUIState.WAITING_FOR_CONFIRMATION
                    GuardianPhase.Complete -> GuardianUIState.COMPLETE
                }
            }

            state = state.copy(
                guardianUIState = guardianUIState,
                guardianState = guardianState ?: state.guardianState,
                invitationId = InvitationId(inviteCode)
            )
        }
    }

    fun createGuardianKey() {
        viewModelScope.launch {
            val guardianEncryptionKey = keyRepository.createGuardianKey()
            guardianRepository.saveKeyInCloud(
                Base58EncodedPrivateKey(
                    Base58.base58Encode(
                        guardianEncryptionKey.privateKeyRaw()
                    )
                )
            )
            state = state.copy(
                guardianEncryptionKey = guardianEncryptionKey
            )
            determineGuardianUIState(state.guardianState)
        }
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = if (userResponse is Resource.Success) {
                determineGuardianUIState(userResponse.data?.guardianStates?.firstOrNull())
                projectLog(message = "User Response: ${userResponse.data}")
                state.copy(
                    userResponse = userResponse,
                )
            } else {
                state.copy(userResponse = userResponse)
            }
        }
    }

    fun acceptGuardianship() {
        state = state.copy(acceptGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val acceptResource = guardianRepository.acceptGuardianship(
                invitationId = state.invitationId,
            )

            state = if (acceptResource is Resource.Success) {
                determineGuardianUIState(acceptResource.data?.guardianState)
                state.copy(
                    acceptGuardianResource = acceptResource
                )
            } else {
                state.copy(acceptGuardianResource = acceptResource)
            }
        }
    }

    fun declineGuardianship() {
        state = state.copy(declineGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val declineResource = guardianRepository.declineGuardianship(
                invitationId = state.invitationId,
            )

            state = if (declineResource is Resource.Success) {
                state.copy(
                    declineGuardianResource = declineResource
                )
            } else {
                state.copy(declineGuardianResource = declineResource)
            }
        }
    }

    fun updateVerificationCode(value: String) {
        if (value.isDigitsOnly()) {
            state = state.copy(verificationCode = value)
            if (state.verificationCode.length == VALID_CODE_LENGTH) {
                submitVerificationCode()
            }
        }

        if (state.submitVerificationResource is Resource.Error) {
            state = state.copy(submitVerificationResource = Resource.Uninitialized)
        }
    }

    fun submitVerificationCode() {
        state = state.copy(submitVerificationResource = Resource.Loading())

        viewModelScope.launch {

            if (state.guardianEncryptionKey == null) {
                loadPrivateKeyFromCloud()
            }

            if (state.invitationId.value.isEmpty()) {
                loadInvitationId()
            }

            val signedVerificationData = guardianRepository.signVerificationCode(
                verificationCode = state.verificationCode,
                state.guardianEncryptionKey!!
            )

            //todo: Have user input this
            val submitVerificationResource = guardianRepository.submitGuardianVerification(
                invitationId = state.invitationId.value,
                submitGuardianVerificationRequest = signedVerificationData
            )

            state = if (submitVerificationResource is Resource.Success) {
                determineGuardianUIState(submitVerificationResource.data?.guardianState)
                state.copy(
                    submitVerificationResource = submitVerificationResource
                )
            } else {
                state.copy(submitVerificationResource = submitVerificationResource, verificationCode = "")
            }
        }
    }

    private suspend fun loadPrivateKeyFromCloud() {
        val privateKeyFromCloud = guardianRepository.retrieveKeyFromCloud()

        val privateKeyRaw = Base58.base58Decode(privateKeyFromCloud.value)

        val recreatedEncryptionKey =
            EncryptionKey.generateFromPrivateKeyRaw(BigInteger(privateKeyRaw))

        state = state.copy(guardianEncryptionKey = recreatedEncryptionKey)
    }

    private suspend fun loadInvitationId() {
        state = state.copy(
            invitationId = when (val guardianState = state.guardianState?.phase) {
                is GuardianPhase.WaitingForCode -> guardianState.invitationId
                is GuardianPhase.WaitingForConfirmation -> guardianState.invitationId
                else -> {
                    InvitationId(guardianRepository.retrieveInvitationId())
                }
            }
        )
    }
}