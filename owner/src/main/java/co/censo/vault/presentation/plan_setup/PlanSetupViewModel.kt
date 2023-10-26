package co.censo.vault.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import Base58EncodedPrivateKey
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
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
        retrieveOwnerState(overwriteUIState = true)

        verificationCodeTimer.startCountDownTimer(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {
            nextTotpTimerTick()
        }

        pollingVerificationTimer.startCountDownTimer(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                retrieveOwnerState(silent = true)
            }
        }
    }

    fun onStop() {
        verificationCodeTimer.stopCountDownTimer()
        pollingVerificationTimer.stopCountDownTimer()
    }


    private fun nextTotpTimerTick() {
        val now = Clock.System.now()
        val updatedCounter = now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
        val secondsLeft = now.epochSeconds - (updatedCounter.times(TotpGenerator.CODE_EXPIRATION))

        state = if (state.counter != updatedCounter) {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
                counter = updatedCounter,
                approverCodes = generateTimeCodes(listOfNotNull(state.primaryApprover, state.backupApprover))
            )
        } else {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
            )
        }
    }

    fun onBackClicked() {
        state = when (state.planSetupUIState) {
            PlanSetupUIState.ApproverActivation -> {
                state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive)
            }

            PlanSetupUIState.EditApproverNickname -> {
                state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation)
            }

            PlanSetupUIState.InviteApprovers,
            PlanSetupUIState.ApproverNickname,
            PlanSetupUIState.ApproverGettingLive,
            PlanSetupUIState.AddBackupApprover,
            PlanSetupUIState.RecoveryInProgress,
            PlanSetupUIState.Completed -> {
                state.copy(navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route))
            }
        }
    }

    private fun retrieveOwnerState(silent: Boolean = false, overwriteUIState: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            updateOwnerState(ownerStateResource.data!!, overwriteUIState)

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState, overwriteUIState: Boolean = false) {
        if (ownerState !is OwnerState.Ready) return

        // update global state
        ownerStateFlow.tryEmit(Resource.Success(ownerState))

        // figure out owner/primary/backup approvers
        val approverSetup = ownerState.guardianSetup?.guardians ?: emptyList()
        val externalApprovers = approverSetup.externalApprovers()
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerApprover()
        val primaryApprover: Guardian.ProspectGuardian? = when {
            externalApprovers.isEmpty() -> null
            externalApprovers.size == 1 -> externalApprovers.first()
            else -> externalApprovers.confirmed().minBy { (it.status as GuardianStatus.Confirmed).confirmedAt }
        }
        val backupApprover: Guardian.ProspectGuardian? = when {
            externalApprovers.isEmpty() -> null
            externalApprovers.size == 1 -> null
            else -> {
                val notConfirmed = externalApprovers.notConfirmed()
                when {
                    notConfirmed.isEmpty() -> externalApprovers.confirmed().maxByOrNull { (it.status as GuardianStatus.Confirmed).confirmedAt }!!
                    else -> notConfirmed.first()
                }

            }
        }

        state = state.copy(
            // approver names are needed on the last screen. Prevent resetting to 'null' after policy is replaced
            ownerApprover = ownerApprover ?: state.ownerApprover,
            primaryApprover = primaryApprover ?: state.primaryApprover,
            backupApprover = backupApprover ?: state.backupApprover,
        )

        // generate codes
        val codes = state.approverCodes.takeIf { totpCodes -> totpCodes.keys.containsAll(
            approverSetup.notConfirmed().map { it.participantId })
        } ?: generateTimeCodes(approverSetup)

        state = state.copy(
            approverCodes = codes,
            ownerState = ownerState,
        )

        // restore UI state on view restart (`overwriteUIState` flag)
        // normally navigation is controlled by pressing "continue" button
        if (overwriteUIState && state.planSetupUIState == PlanSetupUIState.InviteApprovers) {
            if (externalApprovers.notConfirmed().isNotEmpty()) {
                state = state.copy(
                    editedNickname = when (state.approverType) {
                        ApproverType.Primary -> state.primaryApprover?.label
                        ApproverType.Backup -> state.backupApprover?.label
                    } ?: "",
                    planSetupUIState = PlanSetupUIState.ApproverActivation
                )
            } else if (backupApprover?.status is GuardianStatus.Confirmed) {
                initiateRecovery()
            } else if (primaryApprover?.status is GuardianStatus.Confirmed) {
                state = state.copy(planSetupUIState = PlanSetupUIState.AddBackupApprover)
            } else {
                state = state.copy(planSetupUIState = PlanSetupUIState.InviteApprovers)
            }
        }

        // verify approvers
        approverSetup.filter {
            it.status is GuardianStatus.VerificationSubmitted
        }.forEach {
            verifyGuardian(
                it.participantId,
                it.status as GuardianStatus.VerificationSubmitted
            )
        }
    }

    fun onInviteApprover() {
        state = state.copy(
            editedNickname = "",
            planSetupUIState = PlanSetupUIState.ApproverNickname
        )
    }

    fun approverNicknameChanged(nickname: String) {
        state = state.copy(editedNickname = nickname)
    }

    fun onSaveApprover() {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        viewModelScope.launch {
            // policy setup API expects all approvers to be sent with every request
            val updatedPolicySetupGuardians = listOfNotNull(
                state.ownerApprover?.asImplicitlyOwner() ?: createOwnerApprover(),
                state.primaryApprover?.asExternalApprover()
                    ?: createExternalApprover(state.editedNickname),
                if (state.primaryApprover?.status is GuardianStatus.Confirmed) {
                    state.backupApprover?.asExternalApprover()
                        ?: createExternalApprover(state.editedNickname)
                } else {
                    null
                }
            )

            submitPolicySetup(updatedPolicySetupGuardians)
        }
    }

    fun onEditApproverNickname() {
        val nicknameToUpdate = when (state.approverType) {
            ApproverType.Primary -> state.primaryApprover?.label
            ApproverType.Backup -> state.backupApprover?.label
        }

        state = state.copy(
            editedNickname = nicknameToUpdate ?: "",
            planSetupUIState = PlanSetupUIState.EditApproverNickname
        )
    }

    fun onSaveApproverNickname() {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        val ownerApprover = state.ownerApprover?.asImplicitlyOwner()
        val primaryApprover = state.primaryApprover?.asExternalApprover()
        val backupApprover = state.backupApprover?.asExternalApprover()

        val updatedPolicySetupGuardians = when (state.approverType) {
            ApproverType.Primary -> {
                listOfNotNull(
                    ownerApprover,
                    primaryApprover?.copy(label = state.editedNickname),
                    backupApprover
                )
            }

            ApproverType.Backup -> {
                listOfNotNull(
                    ownerApprover,
                    primaryApprover,
                    backupApprover?.copy(label = state.editedNickname),
                )
            }
        }

        submitPolicySetup(updatedPolicySetupGuardians)
    }

    private fun submitPolicySetup(updatedPolicySetupGuardians: List<Guardian.SetupGuardian>) {
        viewModelScope.launch {
            val response = ownerRepository.createPolicySetup(
                threshold = 2U,
                guardians = updatedPolicySetupGuardians
            )

            if (response is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation)

                updateOwnerState(response.data!!.ownerState)
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    private suspend fun createOwnerApprover(): Guardian.SetupGuardian.ImplicitlyOwner {
        val participantId = ParticipantId.generate()
        val approverEncryptionKey = keyRepository.createGuardianKey()

        viewModelScope.async(Dispatchers.IO) {
            keyRepository.saveKeyInCloud(
                key = Base58EncodedPrivateKey(
                    Base58.base58Encode(
                        approverEncryptionKey.privateKeyRaw()
                    )
                ),
                participantId = participantId
            )
        }.await()

        return Guardian.SetupGuardian.ImplicitlyOwner(
            label = "Me",
            participantId = participantId,
            guardianPublicKey = Base58EncodedGuardianPublicKey(approverEncryptionKey.publicExternalRepresentation().value),
        )
    }

    private fun createExternalApprover(nickname: String): Guardian.SetupGuardian.ExternalApprover {
        val participantId = ParticipantId.generate()
        val totpSecret = TotpGenerator.generateSecret()
        val encryptedTotpSecret = keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

        return Guardian.SetupGuardian.ExternalApprover(
            label = nickname,
            participantId = participantId,
            deviceEncryptedTotpSecret = encryptedTotpSecret
        )
    }

    private fun generateTimeCodes(approvers: List<Guardian>): Map<ParticipantId, String> {
        val timeMillis = Clock.System.now().toEpochMilliseconds()

        return approvers.mapNotNull { approver ->
            when {
                approver is Guardian.ProspectGuardian && approver.status.resolveDeviceEncryptedTotpSecret() != null -> {
                    val encryptedTotpSecret = approver.status.resolveDeviceEncryptedTotpSecret()!!

                    val code = TotpGenerator.generateCode(
                        secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
                        counter = timeMillis.div(TotpGenerator.CODE_EXPIRATION)
                    )

                    approver.participantId to code
                }

                else -> null
            }
        }.toMap()
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

    fun saveAndFinish() {
        initiateRecovery()
    }

    fun onGoLiveWithApprover() {
        state = state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation)
    }

    fun onApproverConfirmed() {
        if (state.backupApprover == null) {
            state = state.copy(planSetupUIState = PlanSetupUIState.AddBackupApprover)
        } else {
            initiateRecovery()
        }
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    private fun initiateRecovery() {
        state = state.copy(initiateRecoveryResponse = Resource.Loading())

        viewModelScope.launch {

            // cancel previous recovery if exists
            state.ownerState?.recovery?.let {
                ownerRepository.cancelRecovery()
            }

            // request new recovery without secrets to make shards available immediately
            val initiateRecoveryResponse = ownerRepository.initiateRecovery(listOf())

            if (initiateRecoveryResponse is Resource.Success) {
                // navigate to the facetec view
                state = state.copy(planSetupUIState = PlanSetupUIState.RecoveryInProgress)

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
            val retrieveShardsResponse = ownerRepository.retrieveRecoveryShards(verificationId, biometry)

            if (retrieveShardsResponse is Resource.Success) {
                ownerRepository.cancelRecovery()

                replacePolicy(retrieveShardsResponse.data!!.encryptedShards)
            }

            state = state.copy(retrieveRecoveryShardsResponse = retrieveShardsResponse)

            retrieveShardsResponse.map { it.scanResultBlob }
        }.await()
    }

    private fun replacePolicy(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        state = state.copy(replacePolicyResponse = Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.replacePolicy(
                encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                encryptedMasterPrivateKey = state.ownerState!!.policy.encryptedMasterKey,
                threshold = 2U,
                guardians = listOfNotNull(state.ownerApprover, state.primaryApprover, state.backupApprover)
            )

            state = state.copy(replacePolicyResponse = response)

            if (response is Resource.Success) {
                updateOwnerState(response.data!!.ownerState)

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

    private fun List<Guardian.ProspectGuardian>.ownerApprover(): Guardian.ProspectGuardian? {
        return find { it.status is GuardianStatus.ImplicitlyOwner }
    }

    private fun List<Guardian.ProspectGuardian>.externalApprovers(): List<Guardian.ProspectGuardian> {
        return filter { it.status !is GuardianStatus.ImplicitlyOwner }
    }

    private fun List<Guardian.ProspectGuardian>.confirmed(): List<Guardian.ProspectGuardian> {
        return externalApprovers().filter {
            it.status is GuardianStatus.Confirmed || it.status is GuardianStatus.Onboarded
        }
    }

    private fun List<Guardian.ProspectGuardian>.notConfirmed(): List<Guardian.ProspectGuardian> {
        return externalApprovers().filter {
            it.status !is GuardianStatus.Confirmed && it.status !is GuardianStatus.Onboarded
        }
    }

    private fun Guardian.ProspectGuardian.asExternalApprover(): Guardian.SetupGuardian.ExternalApprover {
        return Guardian.SetupGuardian.ExternalApprover(
            label = this.label,
            participantId = this.participantId,
            deviceEncryptedTotpSecret = this.status.resolveDeviceEncryptedTotpSecret() ?: Base64EncodedData("")
        )
    }

    private fun Guardian.ProspectGuardian.asImplicitlyOwner(): Guardian.SetupGuardian.ImplicitlyOwner {
        return Guardian.SetupGuardian.ImplicitlyOwner(
            label = this.label,
            participantId = this.participantId,
            guardianPublicKey = (this.status as GuardianStatus.ImplicitlyOwner).guardianPublicKey
        )
    }
}


