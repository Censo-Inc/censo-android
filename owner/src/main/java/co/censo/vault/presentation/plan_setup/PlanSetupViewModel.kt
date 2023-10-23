package co.censo.vault.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart() {
        retrieveOwnerState()
        // FIXME listen to the owner state updates instead of api call

        verificationCodeTimer.startCountDownTimer(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {

            val now = Clock.System.now()
            val updatedCounter = now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
            val secondsLeft =
                now.epochSeconds - (updatedCounter.times(TotpGenerator.CODE_EXPIRATION))

            state = if (state.counter != updatedCounter) {
                state.copy(
                    secondsLeft = secondsLeft.toInt(),
                    counter = updatedCounter,
                    approverCodes = generateTimeCodes(state.setupApprovers)
                )
            } else {
                state.copy(
                    secondsLeft = secondsLeft.toInt(),
                )
            }
        }

        pollingVerificationTimer.startCountDownTimer(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                retrieveOwnerState(silent = true)
            }
        }
    }

    fun onBackActionClick() {
        // FIXME
    }

    private fun retrieveOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            if (ownerStateResource is Resource.Success) {
                ownerStateFlow.tryEmit(ownerStateResource)
            }

            state = state.copy(userResponse = ownerStateResource)

            updateOwnerState(ownerStateResource.data!!)
        }
    }

    private fun updateOwnerState(updatedOwnerState: OwnerState) {
        if (updatedOwnerState !is OwnerState.Ready) {
            state = state.copy(planSetupUIState = PlanSetupUIState.InviteApprovers)
            return
        }

        val guardians = updatedOwnerState.guardianSetup?.guardians ?: emptyList()

        val codes = if (state.approverCodes.isEmpty()) {
            generateTimeCodes(guardians)
        } else {
            null
        }

        if (guardians.any {
                it.status !is GuardianStatus.ImplicitlyOwner
                        && it.status !is GuardianStatus.Confirmed
                        && it.status !is GuardianStatus.Onboarded
            }) {
            state = state.copy(
                planSetupUIState = PlanSetupUIState.ApproverActivation
            )
        } else {
            when {
                guardians.filterOwner().size == 2
                        && state.planSetupUIState != PlanSetupUIState.ReShardingSecrets
                        && state.planSetupUIState != PlanSetupUIState.Completed -> {
                    initiateRecovery()
                }

                guardians.filterOwner().size == 1
                        && state.planSetupUIState != PlanSetupUIState.AddBackupApprover
                        && state.planSetupUIState != PlanSetupUIState.ApproverNickname
                        && state.planSetupUIState != PlanSetupUIState.ApproverGettingLive -> {
                    state = state.copy(
                        planSetupUIState = PlanSetupUIState.AddBackupApprover
                    )
                }
            }
        }

        state = state.copy(
            approverCodes = codes ?: state.approverCodes,
            setupApprovers = guardians,
            ownerState = updatedOwnerState,
        )

        guardians.filter {
            it.status is GuardianStatus.VerificationSubmitted
        }.forEach {
            verifyGuardian(
                it.participantId,
                it.status as GuardianStatus.VerificationSubmitted
            )
        }
    }

    fun onInvitePrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.ApproverNickname
        )
    }

    fun approverNicknameChanged(nickname: String) {
        state = state.copy(
            editedNickname = nickname
        )
    }

    fun onSaveApprover() {
        val ownerState: OwnerState.Ready = state.ownerState ?: return

        state = state.copy(
            createPolicySetupResponse = Resource.Loading()
        )

        viewModelScope.launch {
            val policySetupGuardians = ownerState.guardianSetup?.guardians ?: emptyList()
            
            val updatedPolicySetupGuardians = listOfNotNull(
                resolveOrCreateOwnerApprover(policySetupGuardians),
                resolveOrCreatePrimaryApprover(policySetupGuardians),
                skipOrCreateBackupApprover(policySetupGuardians)
            )
            
            val response = ownerRepository.createPolicySetup(
                threshold = 2U,
                guardians = updatedPolicySetupGuardians
            )

            if (response is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive)

                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    private suspend fun resolveOrCreateOwnerApprover(setupGuardians: List<Guardian.ProspectGuardian>): Guardian.SetupGuardian.ImplicitlyOwner {
        return when (val existingOwner = setupGuardians.find { it.status is GuardianStatus.ImplicitlyOwner }) {

            null -> {
                val participantId = ParticipantId.generate()
                val approverEncryptionKey = keyRepository.createGuardianKey()

                // TODO saveKeyInCloud overrides previous key
                // TODO uncomment when key are stored per participantIs
                /*keyRepository.saveKeyInCloud(
                    key = Base58EncodedPrivateKey(
                        Base58.base58Encode(
                            approverEncryptionKey.privateKeyRaw()
                        )
                    ),
                    participantId = participantId
                )*/

                Guardian.SetupGuardian.ImplicitlyOwner(
                    label = "Me",
                    participantId = participantId,
                    guardianPublicKey = Base58EncodedGuardianPublicKey(approverEncryptionKey.publicExternalRepresentation().value),
                )
            }

            else -> {
                Guardian.SetupGuardian.ImplicitlyOwner(
                    label = existingOwner.label,
                    participantId = existingOwner.participantId,
                    guardianPublicKey = (existingOwner.status as GuardianStatus.ImplicitlyOwner).guardianPublicKey
                )
            }
        }
    }

    private fun resolveOrCreatePrimaryApprover(setupGuardians: List<Guardian.ProspectGuardian>): Guardian.SetupGuardian.ExternalApprover {
        val externalApprovers = setupGuardians.filterOwner()

        return if (externalApprovers.isEmpty()) {
            createExternalApprover(state.editedNickname)
        } else {
            externalApprovers.filterNotCompleted().first().toExternalApprover()
        }
    }

    private fun Guardian.ProspectGuardian.toExternalApprover(): Guardian.SetupGuardian.ExternalApprover {
        return Guardian.SetupGuardian.ExternalApprover(
            label = this.label,
            participantId = this.participantId,
            deviceEncryptedTotpSecret = this.status.resolveDeviceEncryptedTotpSecret()
                ?: Base64EncodedData("") // FIXME deviceEncryptedTotpSecret is not always available
        )
    }

    private fun skipOrCreateBackupApprover(setupGuardians: List<Guardian.ProspectGuardian>): Guardian.SetupGuardian.ExternalApprover? {
        val externalApprovers = setupGuardians.filterOwner()

        // primary is already confirmed
        return if (externalApprovers.size == 1 && externalApprovers.any { it.status is GuardianStatus.Confirmed }) {
            createExternalApprover(state.editedNickname)
        } else {
            null  // to early for backup approver
        }
    }

    private fun createExternalApprover(nickname: String): Guardian.SetupGuardian.ExternalApprover {
        val participantId = ParticipantId.generate()
        val totpSecret = TotpGenerator.generateSecret()
        val encryptedTotpSecret =
            keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

        return Guardian.SetupGuardian.ExternalApprover(
            label = nickname,
            participantId = participantId,
            deviceEncryptedTotpSecret = encryptedTotpSecret
        )
    }

    private fun generateTimeCodes(guardians: List<Guardian>): Map<ParticipantId, String> {
        val timeMillis = Clock.System.now().toEpochMilliseconds()

        return guardians
            .filter {
                it is Guardian.ProspectGuardian && (it.status.resolveDeviceEncryptedTotpSecret() != null)
            }.mapNotNull {

                when (it) {
                    is Guardian.ProspectGuardian -> {
                        val totpSecret = it.status.resolveDeviceEncryptedTotpSecret()?.base64Encoded

                        val code = TotpGenerator.generateCode(
                            secret = String(
                                keyRepository.decryptWithDeviceKey(
                                    Base64.getDecoder().decode(totpSecret)
                                )
                            ),
                            counter = timeMillis.div(TotpGenerator.CODE_EXPIRATION)
                        )

                        it.participantId to code
                    }

                    else -> null
                }
            }
            .toMap()
    }

    private fun verifyGuardian(
        participantId: ParticipantId,
        guardianStatus: GuardianStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            verificationCode = state.approverCodes[participantId] ?: "",
            transportKey = guardianStatus.guardianPublicKey,
            signature = guardianStatus.signature,
            timeMillis = guardianStatus.timeMillis
        )

        viewModelScope.launch {
            if (codeVerified) {

                val keyConfirmationTimeMillis = Clock.System.now().epochSeconds

                val keyConfirmationMessage =
                    guardianStatus.guardianPublicKey.getBytes() + participantId.getBytes() + keyConfirmationTimeMillis.toString()
                        .toByteArray()
                val keyConfirmationSignature =
                    keyRepository.retrieveInternalDeviceKey().sign(keyConfirmationMessage)

                val confirmGuardianShipResponse = ownerRepository.confirmGuardianShip(
                    participantId = participantId,
                    keyConfirmationSignature = keyConfirmationSignature,
                    keyConfirmationTimeMillis = keyConfirmationTimeMillis
                )

                if (confirmGuardianShipResponse is Resource.Success) {
                    updateOwnerState(confirmGuardianShipResponse.data!!.ownerState)
                }
            } else {
                val rejectVerificationResponse = ownerRepository.rejectVerification(participantId)

                if (rejectVerificationResponse is Resource.Success) {
                    updateOwnerState(rejectVerificationResponse.data!!.ownerState)
                }
            }
        }
    }

    fun onInviteBackupApprover() {
        state = state.copy(
            editedNickname = "",
            planSetupUIState = PlanSetupUIState.ApproverNickname
        )
    }

    fun saveAndFinish() {
        state = state.copy(planSetupUIState = PlanSetupUIState.ReShardingSecrets)
    }

    fun onGoLiveWithApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.ApproverActivation
        )
    }

    fun onApproverConfirmed() {
        val nextStep = if (state.setupApprovers.size == 2) {
            PlanSetupUIState.AddBackupApprover
        } else {
            PlanSetupUIState.ReShardingSecrets
        }

        state = state.copy(planSetupUIState = nextStep)
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun initiateRecovery() {
        state = state.copy(initiateRecoveryResponse = Resource.Loading())

        viewModelScope.launch {

            // cancel previous recovery if exists
            state.ownerState?.recovery?.let { // FIXME populate state earlier
                ownerRepository.cancelRecovery()
            }

            // request new recovery without secrets
            val initiateRecoveryResponse = ownerRepository.initiateRecovery(listOf())

            if (initiateRecoveryResponse is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.ReShardingSecrets)

                updateOwnerState(initiateRecoveryResponse.data!!.ownerState)
            }

            state = state.copy(initiateRecoveryResponse = initiateRecoveryResponse)
        }
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(retrieveRecoveryShardsResponse = Resource.Loading())

        return viewModelScope.async {
            // no secrets to be fetched
            val retrieveShardsResponse = ownerRepository.retrieveRecoveryShards(verificationId, biometry)

            state = state.copy(retrieveRecoveryShardsResponse = retrieveShardsResponse)

            if (retrieveShardsResponse is Resource.Success) {
                reShardAndSubmit(retrieveShardsResponse.data!!.encryptedShards)
            }

            retrieveShardsResponse.map { it.scanResultBlob }
        }.await()
    }

    fun reShardAndSubmit(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        state = state.copy(replacePolicyResponse = Resource.Loading())

        viewModelScope.launch {

            ownerRepository.cancelRecovery()

            val response = ownerRepository.replacePolicy(
                encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                encryptedMasterPrivateKey = state.ownerState!!.policy.encryptedMasterKey,
                2U,
                guardians = state.setupApprovers
            )

            state = state.copy(
                replacePolicyResponse = response
            )

            if (response is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.Completed)
            }
        }
    }

    fun onFullyCompleted() {
        state = state.copy(navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route))
    }

    fun reset() {
        state = PlanSetupState()
    }

    private fun List<Guardian.ProspectGuardian>.filterOwner(): List<Guardian.ProspectGuardian> {
        return filter {
            it.status !is GuardianStatus.ImplicitlyOwner
        }
    }

    private fun List<Guardian.ProspectGuardian>.filterNotCompleted(): List<Guardian.ProspectGuardian> {
        return filter {
            it.status is GuardianStatus.Confirmed || it.status is GuardianStatus.Onboarded
        }
    }

}