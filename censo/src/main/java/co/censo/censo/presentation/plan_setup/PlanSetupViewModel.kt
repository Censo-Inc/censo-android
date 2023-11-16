package co.censo.censo.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import co.censo.shared.data.model.RecoveryIntent
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.initial_plan_setup.InitialKeyData
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiRequest
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.projectLog
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject


/**
 * User Actions
 *
 * onBackClicked: Not part of major flow. Move user back in flow.
 *
 * onApproverNicknameChanged: update either primary or secondary approver nickname
 * onEditApproverNickname: User needs to update an already entered nickname
 * onSaveApproverNickname:
 *      User will save the approver nickname, and then we trigger submitPolicySetup.
 *      submitPolicySetup can be submitted multiple times, so a user could add a secondary approver,
 *      or finish and create their plan.
 * onInviteApprover:
 *      Most times this will send us to create approver nickname
 *      If both approvers have nicknames, we will go directly to Continue Live Holding Screen
 * onSaveApprover:
 *      If ownerApprover is made, call submitPolicySetup
 *      Else create owner approver and upload key
 *      I don't think we need 2 methods for onSaveApprover and onSaveApproverNickname
 * onSaveAndFinish
 *      Badly named method
 * onGoLiveWithApprover: Move us to Approver Activation UI
 * onApproverConfirmed:
 *      If primary approver: Send user to Add Alternate Approver
 *      If alternate approver: Start recovery to create new plan
 * onFullyCompleted: Send user home
 * onSaveAndFinishPlan: User done modifying the plan
 *      If we never confirmed the alternate approver, submit new policy, then initiate recovery
 *      If confirmed alternate approver, then initiate recovery
 *
 *
 * Internal Methods
 *
 * submitNewPolicy: Done multiple times. Anytime we need to create a new policy.
 *
 * initiateRecovery: Finalized the plan setup, and need to do recovery to re-shard and finalize plan.
 *      API call to initiate recovery. If that is a success, send user to Facetec.
 *
 * faceScanReady: Face scan completed externally in FacetecAuth and we will now call replacePolicy
 *
 * replacePolicy: Replace existing policy, and finalize plan
 */


@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer,
) : ViewModel() {
    var state by mutableStateOf(PlanSetupState())
        private set

    //region Lifecycle Methods
    fun onStart() {
        if (state.planSetupUIState == PlanSetupUIState.Initial) {
            state = state.copy(planSetupUIState = PlanSetupUIState.ApproverNickname)
        }

        viewModelScope.launch {
            val ownerState = ownerStateFlow.value
            if (ownerState is Resource.Success) {
                updateOwnerState(ownerState.data!!, overwriteUIState = true)
            }

            pollingVerificationTimer.startWithDelay(
                initialDelay = CountDownTimerImpl.Companion.INITIAL_DELAY,
                interval = CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
            ) {
                if (state.userResponse !is Resource.Loading) {
                    retrieveOwnerState(silent = true)
                }
            }
        }

        verificationCodeTimer.start(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {
            nextTotpTimerTick()
        }
    }

    fun onStop() {
        verificationCodeTimer.stop()
        pollingVerificationTimer.stop()
    }
    //endregion

    //region User Actions
    fun onBackClicked() {
        val backIconNavigation = listOf(
            PlanSetupUIState.EditApproverNickname to PlanSetupUIState.ApproverActivation,
            PlanSetupUIState.ApproverActivation to PlanSetupUIState.ApproverGettingLive,
            PlanSetupUIState.ApproverGettingLive to PlanSetupUIState.AddAlternateApprover,
        ).toMap()

        when (state.backArrowType) {
            PlanSetupState.BackIconType.None -> {}

            PlanSetupState.BackIconType.Back -> {
                state = state.copy(
                    planSetupUIState = backIconNavigation[state.planSetupUIState] ?: state.planSetupUIState
                )
            }

            PlanSetupState.BackIconType.Exit -> {
                state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
            }
        }
    }

    fun onInviteApprover() {
        state = if (state.alternateApprover != null) {
            // skip name entry of alternate approver if it is already set
            state.copy(
                planSetupUIState = PlanSetupUIState.ApproverGettingLive
            )
        } else {
            state.copy(
                editedNickname = "",
                planSetupUIState = PlanSetupUIState.ApproverNickname
            )
        }
    }

    fun onApproverNicknameChanged(nickname: String) {
        state = state.copy(
            editedNickname = nickname
        )
    }

    fun onSaveApprover() {
        val ownerAsApprover = state.ownerApprover?.asOwnerAsApprover() ?: createOwnerApprover()

        //Can move directly to setting up and submitting policy
        state = state.copy(createPolicySetupResponse = Resource.Loading())
        submitPolicySetup(
            updatedPolicySetupGuardians = getUpdatedPolicySetupGuardianList(ownerAsApprover)
        )
    }

    fun onEditApproverNickname() {
        val nicknameToUpdate = when (state.approverType) {
            ApproverType.Primary -> state.primaryApprover?.label
            ApproverType.Alternate -> state.alternateApprover?.label
        }

        state = state.copy(
            editedNickname = nicknameToUpdate ?: "",
            planSetupUIState = PlanSetupUIState.EditApproverNickname
        )
    }

    fun onSaveApproverNickname() {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        val ownerApprover = state.ownerApprover?.asOwnerAsApprover()
        val primaryApprover = state.primaryApprover?.asExternalApprover()
        val alternateApprover = state.alternateApprover?.asExternalApprover()

        val updatedPolicySetupGuardians = when (state.approverType) {
            ApproverType.Primary -> {
                listOfNotNull(
                    ownerApprover,
                    primaryApprover?.copy(label = state.editedNickname),
                    alternateApprover
                )
            }

            ApproverType.Alternate -> {
                listOfNotNull(
                    ownerApprover,
                    primaryApprover,
                    alternateApprover?.copy(label = state.editedNickname),
                )
            }
        }

        submitPolicySetup(updatedPolicySetupGuardians)
    }

    fun onSaveAndFinishPlan() {
        if (state.alternateApprover != null) {
            // finishing flow after primary approver
            dropAlternateApproverAndInitiateRecovery()
        } else {
            saveKeyWithEntropy()
        }
    }

    fun onGoLiveWithApprover() {
        state = state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation)
    }

    fun onApproverConfirmed() {
        if (state.alternateApprover == null) {
            state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover)
        } else {
            initiateRecovery()
        }
    }

    fun onFullyCompleted() {
        state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
    }
    //endregion

    //region Internal Methods
    private fun nextTotpTimerTick() {
        val now = Clock.System.now()
        val updatedCounter = now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
        val secondsLeft = now.epochSeconds - (updatedCounter.times(TotpGenerator.CODE_EXPIRATION))

        state = if (state.counter != updatedCounter) {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
                counter = updatedCounter,
                approverCodes = generateTimeCodes(listOfNotNull(state.primaryApprover, state.alternateApprover))
            )
        } else {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
            )
        }
    }

    private fun retrieveOwnerState(silent: Boolean = false, overwriteUIState: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            ownerStateResource.data?.let {
                updateOwnerState(it, overwriteUIState)
            }

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState, overwriteUIState: Boolean = false) {
        if (ownerState !is OwnerState.Ready) return

        // update global state
        ownerStateFlow.tryEmit(Resource.Success(ownerState))

        // figure out owner/primary/alternate approvers
        val approverSetup = ownerState.guardianSetup?.guardians ?: emptyList()
        val externalApprovers = approverSetup.externalApprovers()
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerAsApprovers()
        val primaryApprover: Guardian.ProspectGuardian? = when {
            externalApprovers.isEmpty() -> null
            externalApprovers.size == 1 -> externalApprovers.first()
            else -> externalApprovers.confirmed().minBy { (it.status as GuardianStatus.Confirmed).confirmedAt }
        }
        val alternateApprover: Guardian.ProspectGuardian? = when {
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
            alternateApprover = alternateApprover ?: state.alternateApprover,
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
        if (overwriteUIState && state.planSetupUIState == PlanSetupUIState.ApproverNickname) {
            if (externalApprovers.notConfirmed().isNotEmpty()) {
                state = state.copy(
                    editedNickname = when (state.approverType) {
                        ApproverType.Primary -> state.primaryApprover?.label
                        ApproverType.Alternate -> state.alternateApprover?.label
                    } ?: "",
                    planSetupUIState = PlanSetupUIState.ApproverActivation
                )
            } else if (alternateApprover?.status is GuardianStatus.Confirmed) {
                initiateRecovery()
            } else if (primaryApprover?.status is GuardianStatus.Confirmed) {
                state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover)
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

    private fun createOwnerApprover() : Guardian.SetupGuardian.OwnerAsApprover {
        val participantId = ParticipantId.generate()

        val ownerAsApprover = Guardian.SetupGuardian.OwnerAsApprover(
            label = "Me",
            participantId = participantId,
        )

        state = state.copy(ownerParticipantId = participantId)

        return ownerAsApprover
    }

    private fun getUpdatedPolicySetupGuardianList(ownerApprover: Guardian.SetupGuardian.OwnerAsApprover): List<Guardian.SetupGuardian> =
        listOfNotNull(
            ownerApprover,
            state.primaryApprover?.asExternalApprover()
                ?: createExternalApprover(state.editedNickname),
            if (state.primaryApprover?.status is GuardianStatus.Confirmed) {
                state.alternateApprover?.asExternalApprover()
                    ?: createExternalApprover(state.editedNickname)
            } else {
                null
            }
        )

    private fun createPolicySetupWithOwnerApprover(ownerApprover: Guardian.SetupGuardian.OwnerAsApprover) {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        viewModelScope.launch {
            submitPolicySetup(
                updatedPolicySetupGuardians = getUpdatedPolicySetupGuardianList(ownerApprover)
            )
        }
    }

    //This needs to happen before we save any key information
    private fun submitPolicySetup(updatedPolicySetupGuardians: List<Guardian.SetupGuardian>) {
        viewModelScope.launch {
            val response = ownerRepository.createPolicySetup(
                threshold = 2U,
                guardians = updatedPolicySetupGuardians
            )

            if (response is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive)

                updateOwnerState(response.data!!.ownerState)
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
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

        return approvers.mapNotNull { approver ->
            when {
                approver is Guardian.ProspectGuardian && approver.status.resolveDeviceEncryptedTotpSecret() != null -> {
                    val encryptedTotpSecret = approver.status.resolveDeviceEncryptedTotpSecret()!!

                    val code = TotpGenerator.generateCode(
                        secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
                        counter = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION )
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
            encryptedTotpSecret = guardianStatus.deviceEncryptedTotpSecret,
            transportKey = guardianStatus.guardianPublicKey,
            signature = guardianStatus.signature,
            timeMillis = guardianStatus.timeMillis
        )

        viewModelScope.launch {
            if (codeVerified) {

                val keyConfirmationTimeMillis = Clock.System.now().toEpochMilliseconds()

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

    private fun dropAlternateApproverAndInitiateRecovery() {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.createPolicySetup(
                threshold = 2U,
                guardians = listOfNotNull(
                    state.ownerApprover?.asOwnerAsApprover(),
                    state.primaryApprover?.asExternalApprover()
                )
            )

            if (response is Resource.Success) {
                state = state.copy(alternateApprover = null)
                saveKeyWithEntropy()
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    private fun saveKeyWithEntropy() {
        state = state.copy(saveKeyToCloud = Resource.Loading())
        val participantId = ParticipantId.generate()
        val approverEncryptionKey = keyRepository.createGuardianKey()

        val approverSetup = state.ownerState?.guardianSetup?.guardians ?: emptyList()
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerAsApprovers()

        val entropy = (ownerApprover?.status as? GuardianStatus.OwnerAsApprover)?.entropy!!

        val idToken = keyRepository.retrieveSavedDeviceId()

        val encryptedKey = approverEncryptionKey.encryptWithEntropy(
            deviceKeyId = idToken,
            entropy = entropy
        )

        val publicKey = Base58EncodedGuardianPublicKey(
            approverEncryptionKey.publicExternalRepresentation().value
        )


        val keyData = PlanSetupKeyData(
            encryptedPrivateKey = encryptedKey,
            publicKey = publicKey
        )

        state = state.copy(keyData = keyData)

        state = state.copy(
            keyData = keyData,
            ownerParticipantId = participantId,
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
            )
        )
    }


    private fun completeGuardianOwnership() {
        state = state.copy(completeGuardianShipResponse = Resource.Loading())

        viewModelScope.launch {

            val completeOwnerGuardianshipApiRequest =
                CompleteOwnerGuardianshipApiRequest(
                    guardianPublicKey = state.keyData?.publicKey!!
                )

            val approverSetup = state.ownerState?.guardianSetup?.guardians ?: emptyList()
            val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerAsApprovers()

            val partId = ownerApprover?.participantId!!

            val completeGuardianShipResponse = ownerRepository.completeGuardianOwnership(
                partId,
                completeOwnerGuardianshipApiRequest
            )

            if (completeGuardianShipResponse is Resource.Success) {
                updateOwnerState(completeGuardianShipResponse.data!!.ownerState)
                initiateRecovery()
            }

            state = state.copy(
                completeGuardianShipResponse = completeGuardianShipResponse
            )
        }
    }

    private fun initiateRecovery() {
        state = state.copy(initiateRecoveryResponse = Resource.Loading())

        viewModelScope.launch {

            // cancel previous recovery if exists
            state.ownerState?.recovery?.let {
                ownerRepository.cancelRecovery()
            }

            // request new recovery for policy replacement
            val initiateRecoveryResponse = ownerRepository.initiateRecovery(RecoveryIntent.ReplacePolicy)

            if (initiateRecoveryResponse is Resource.Success) {
                // navigate to the facetec view
                state = state.copy(planSetupUIState = PlanSetupUIState.RecoveryInProgress)

                updateOwnerState(initiateRecoveryResponse.data!!.ownerState)
            }

            state = state.copy(initiateRecoveryResponse = initiateRecoveryResponse)
        }
    }

    private fun replacePolicy(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Loading())
        if (state.ownerState!!.guardianSetup!!.guardians.any {  !ownerRepository.verifyKeyConfirmationSignature(it) }) {
            state = state.copy(verifyKeyConfirmationSignature = Resource.Error())
            return
        }

        state = state.copy(replacePolicyResponse = Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.replacePolicy(
                encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                encryptedMasterPrivateKey = state.ownerState!!.policy.encryptedMasterKey,
                threshold = 2U,
                guardians = listOfNotNull(state.ownerApprover, state.primaryApprover, state.alternateApprover)
            )

            state = state.copy(replacePolicyResponse = response)

            if (response is Resource.Success) {
                updateOwnerState(response.data!!.ownerState)

                state = state.copy(planSetupUIState = PlanSetupUIState.Completed)
            }
        }
    }
    //endregion

    //region FaceScan
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
    //endregion

    //region Cloud Storage
    fun onKeyUploadSuccess() {
        resetCloudStorageActionState()
        completeGuardianOwnership()
    }

    fun onKeyUploadFailed(exception: Exception?) {
        state = state.copy(
            createPolicySetupResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudUpload)
    }
    //endregion

    //region Reset functions
    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun reset() {
        state = PlanSetupState()
        retrieveOwnerState(silent = false, overwriteUIState = true)
    }

    private fun resetCloudStorageActionState() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            saveKeyToCloud = Resource.Uninitialized
        )
    }

    fun resetVerifyKeyConfirmationSignature() {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Uninitialized)
    }
    //endregion

    //region Extension Functions Mapping Approver Types

    private fun List<Guardian.ProspectGuardian>.ownerAsApprovers(): Guardian.ProspectGuardian? {
        return find { it.status is GuardianStatus.OwnerAsApprover || it.status is GuardianStatus.ImplicitlyOwner }
    }
    private fun List<Guardian.ProspectGuardian>.implicitOwners(): Guardian.ProspectGuardian? {
        return find { it.status is GuardianStatus.ImplicitlyOwner }
    }

    private fun List<Guardian.ProspectGuardian>.externalApprovers(): List<Guardian.ProspectGuardian> {
        return filter { it.status !is GuardianStatus.OwnerAsApprover && it.status !is GuardianStatus.ImplicitlyOwner }
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

    private fun Guardian.ProspectGuardian.asOwnerAsApprover(): Guardian.SetupGuardian.OwnerAsApprover {
        return Guardian.SetupGuardian.OwnerAsApprover(
            label = this.label,
            participantId = this.participantId,
        )
    }

    private fun Guardian.ProspectGuardian.asImplicitlyOwner(): Guardian.SetupGuardian.ImplicitlyOwner {
        return Guardian.SetupGuardian.ImplicitlyOwner(
            label = this.label,
            participantId = this.participantId,
            guardianPublicKey = (this.status as GuardianStatus.ImplicitlyOwner).guardianPublicKey
        )
    }
    //endregion
}

sealed interface PlanSetupAction {
    data class ApproverNicknameChange(val name: String) : PlanSetupAction
    object EditApproverNickname : PlanSetupAction
    object SaveApproverNickname : PlanSetupAction
    object InviteApprover : PlanSetupAction
    object GoLiveWithApprover : PlanSetupAction
    object ApproverConfirmed : PlanSetupAction
    object SaveAndFinishPlan: PlanSetupAction
    object Complete: PlanSetupAction
}