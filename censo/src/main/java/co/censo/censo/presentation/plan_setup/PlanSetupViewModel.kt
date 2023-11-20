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
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiRequest
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject


/**
 *
 * Main Processes:
 *
 * Process 1: Setup Approvers: (Primary and Alternate)
 *      Set the user nickname
 *          (edit user nickname)
 *      Activate Approver
 *      Create Setup Policy as we move forward
 *          Called multiple times as we are adding each approver
 *
 *
 * Process 2: Create Owner Approver Key
 *      Create key locally
 *      Encrypt it with entropy
 *      Save it to cloud
 *      Check owner user is finalized
 *          Once we key saved in the cloud, we need to upload
 *          the public key for the owner to backend.
 *
 *
 * Process 3: Complete recovery to set new plan
 *      Complete facetec to get biometry data back
 *      Retrieve shards from backend
 *      Replace Policy
 *
 * User Actions
 *
 * onBackClicked: Not part of major flow. Move user back in flow.
 *
 * onApproverNicknameChanged:
 *      Update either primary or secondary approver nickname
 *
 * onEditApproverNickname:
 *      User needs to update an already entered nickname
 *      Will exit by saving approver and creating setup policy
 *
 * onSaveApproverNickname:
 *      Same as onEditApproverNickname, except we know nickname is set
 *      Will exit by saving approver and creating setup policy
 *
 * onInviteAlternateApprover:
 *      If we already have data for alternate approver, then we go to get live
 *      If we don't have info on alternate approver, then we need to create their nickname
 *
 * saveApproverAndSubmitPolicy:
 *      Submit policy setup with current approver set
 *
 * updateApproverNicknameAndSubmitPolicy
 *      Update approver nickname on state approver that was being edited
 *      Submit policy setup
 *
 * onGoLiveWithApprover: Simple method to move us to Approver Activation UI.
 *
 * onApproverConfirmed:
 *      When an approver TOTP has been verified
 *      If primary approver: Send user to Add Alternate Approver
 *      If alternate approver: Start recovery to create new plan
 *
 * onFullyCompleted: Send user home. They have setup plan.
 *
 * onSaveAndFinishPlan: User done modifying the plan
 *      If we never confirmed the alternate approver, submit new policy, then initiate recovery
 *      If confirmed alternate approver, then initiate recovery
 *
 *
 * Internal Methods
 *
 * updateOwnerState: Called anytime we get new owner state from backend.
 *      Sets all state data for the 3 approvers
 *      Generates TOTP codes if needed
 *      Does necessary navigation if needed
 *          Don't have strong grasp on this
 *      Veriify any approvers if needed
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

    //region Events
    fun receivePlanAction(action: PlanSetupAction) {
        when (action) {
            //Back
            PlanSetupAction.BackClicked -> onBackClicked()

            //Retry
            PlanSetupAction.Retry -> retrieveOwnerState(silent = false)

            //Nickname or Approver Actions
            is PlanSetupAction.ApproverNicknameChanged ->
                onApproverNicknameChanged(action.name)
            PlanSetupAction.ApproverConfirmed -> onApproverConfirmed()
            PlanSetupAction.EditApproverNickname -> onEditApproverNickname()
            PlanSetupAction.GoLiveWithApprover -> onGoLiveWithApprover()
            PlanSetupAction.InviteApprover -> onInviteAlternateApprover()
            PlanSetupAction.SaveApproverAndSavePolicy -> saveApproverAndSubmitPolicy()
            PlanSetupAction.EditApproverAndSavePolicy -> updateApproverNicknameAndSubmitPolicy()


            //Cloud Actions
            PlanSetupAction.KeyUploadSuccess -> onKeyUploadSuccess()
            is PlanSetupAction.KeyDownloadSuccess -> onKeyDownloadSuccess(action.encryptedKey)

            is PlanSetupAction.KeyDownloadFailed -> onKeyDownloadFailed(action.e)
            is PlanSetupAction.KeyUploadFailed -> onKeyUploadFailed(action.e)

            //Finalizing Actions
            PlanSetupAction.SavePlan -> onSaveAndFinishPlan()
            PlanSetupAction.Completed -> onFullyCompleted()
        }
    }
    //endregion

    //region Lifecycle Methods
    fun onStart() {
        if (state.planSetupUIState == PlanSetupUIState.Initial_1) {
            state = state.copy(planSetupUIState = PlanSetupUIState.ApproverNickname_2)
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
    private fun onBackClicked() {
        val backIconNavigation = listOf(
            PlanSetupUIState.EditApproverNickname_3 to PlanSetupUIState.ApproverActivation_5,
            PlanSetupUIState.ApproverActivation_5 to PlanSetupUIState.ApproverGettingLive_4,
            PlanSetupUIState.ApproverGettingLive_4 to PlanSetupUIState.AddAlternateApprover_6,
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

    private fun onInviteAlternateApprover() {
        state = if (state.alternateApprover != null) {
            // skip name entry of alternate approver if it is already set
            state.copy(
                planSetupUIState = PlanSetupUIState.ApproverGettingLive_4
            )
        } else {
            state.copy(
                editedNickname = "",
                planSetupUIState = PlanSetupUIState.ApproverNickname_2
            )
        }
    }

    private fun onApproverNicknameChanged(nickname: String) {
        state = state.copy(
            editedNickname = nickname
        )
    }

    private fun saveApproverAndSubmitPolicy() {
        val ownerAsApprover = state.ownerApprover?.asOwnerAsApprover() ?: createOwnerApprover()

        //Can move directly to setting up and submitting policy
        state = state.copy(createPolicySetupResponse = Resource.Loading())
        submitPolicySetup(
            updatedPolicySetupGuardians = getUpdatedPolicySetupGuardianList(ownerAsApprover)
        )
    }

    private fun onEditApproverNickname() {
        val nicknameToUpdate = when (state.approverType) {
            ApproverType.Primary -> state.primaryApprover?.label
            ApproverType.Alternate -> state.alternateApprover?.label
        }

        state = state.copy(
            editedNickname = nicknameToUpdate ?: "",
            planSetupUIState = PlanSetupUIState.EditApproverNickname_3
        )
    }

    private fun updateApproverNicknameAndSubmitPolicy() {
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

    private fun onSaveAndFinishPlan() {
        if (state.alternateApprover != null) {
            // finishing flow after primary approver
            dropAlternateApproverAndSaveKeyWithEntropy()
        } else {
            checkUserHasSavedKeyAndSubmittedPolicy()
        }
    }

    private fun onGoLiveWithApprover() {
        state = state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation_5)
    }

    private fun onApproverConfirmed() {
        if (state.alternateApprover == null) {
            state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover_6)
        } else {
            checkUserHasSavedKeyAndSubmittedPolicy()
        }
    }

    private fun onFullyCompleted() {
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
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerApprovers()
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
        if (overwriteUIState && state.planSetupUIState == PlanSetupUIState.ApproverNickname_2) {
            if (externalApprovers.notConfirmed().isNotEmpty()) {
                state = state.copy(
                    editedNickname = when (state.approverType) {
                        ApproverType.Primary -> state.primaryApprover?.label
                        ApproverType.Alternate -> state.alternateApprover?.label
                    } ?: "",
                    planSetupUIState = PlanSetupUIState.ApproverActivation_5
                )
            } else if (alternateApprover?.status is GuardianStatus.Confirmed) {
                checkUserHasSavedKeyAndSubmittedPolicy()
            } else if (primaryApprover?.status is GuardianStatus.Confirmed) {
                state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover_6)
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

    private fun createOwnerApprover(): Guardian.SetupGuardian.OwnerAsApprover {
        val participantId = ParticipantId.generate()

        return Guardian.SetupGuardian.OwnerAsApprover(
            label = "Me",
            participantId = participantId,
        )
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

    //This needs to happen before we save any key information
    private fun submitPolicySetup(updatedPolicySetupGuardians: List<Guardian.SetupGuardian>) {
        viewModelScope.launch {
            val response = ownerRepository.createPolicySetup(
                threshold = 2U,
                guardians = updatedPolicySetupGuardians
            )

            if (response is Resource.Success) {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive_4)

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

    private fun dropAlternateApproverAndSaveKeyWithEntropy() {
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
                checkUserHasSavedKeyAndSubmittedPolicy()
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    private fun saveKeyWithEntropy() {
        state = state.copy(saveKeyToCloud = Resource.Loading())
        val approverEncryptionKey = keyRepository.createGuardianKey()

        val approverSetup = state.ownerState?.guardianSetup?.guardians ?: emptyList()
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerApprovers()

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

        state = state.copy(
            keyData = keyData,
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
            val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerApprovers()

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

    private fun checkUserHasSavedKeyAndSubmittedPolicy() {
        val owner = state.ownerApprover

        if (owner == null) {
            retrieveOwnerState()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            if (owner.status is GuardianStatus.ImplicitlyOwner) {
                initiateRecovery()
                return@launch
            }

            val loadedKey =
                state.keyData?.encryptedPrivateKey != null && state.keyData?.publicKey != null

            if (!loadedKey) {
                if (!keyRepository.userHasKeySavedInCloud(owner.participantId)) {
                    state = state.copy(
                        cloudStorageAction = CloudStorageActionData(
                            triggerAction = true, action = CloudStorageActions.DOWNLOAD
                        ),
                    )
                } else {
                    saveKeyWithEntropy()
                }

                return@launch
            }


            completeGuardianOwnership()
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
                state = state.copy(planSetupUIState = PlanSetupUIState.RecoveryInProgress_7)

                updateOwnerState(initiateRecoveryResponse.data!!.ownerState)
            }

            state = state.copy(initiateRecoveryResponse = initiateRecoveryResponse)
        }
    }

    private fun replacePolicy(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Loading())

        try {

            if (state.ownerState!!.guardianSetup!!.guardians.any {
                    !ownerRepository.verifyKeyConfirmationSignature(
                        it
                    )
                }) {
                state = state.copy(verifyKeyConfirmationSignature = Resource.Error())
                return
            }

            state = state.copy(replacePolicyResponse = Resource.Loading())

            viewModelScope.launch(Dispatchers.IO) {
                val response = ownerRepository.replacePolicy(
                    encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                    encryptedMasterPrivateKey = state.ownerState!!.policy.encryptedMasterKey,
                    threshold = 2U,
                    guardians = listOfNotNull(
                        state.ownerApprover,
                        state.primaryApprover,
                        state.alternateApprover
                    )
                )

                state = state.copy(replacePolicyResponse = response)

                if (response is Resource.Success) {
                    updateOwnerState(response.data!!.ownerState)

                    state = state.copy(planSetupUIState = PlanSetupUIState.Completed_8)
                }
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicy)
            state = state.copy(replacePolicyResponse = Resource.Error(exception = e))
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
    private fun onKeyUploadSuccess() {
        resetCloudStorageActionState()
        completeGuardianOwnership()
    }

    private fun onKeyDownloadSuccess(encryptedKey: ByteArray) {
        resetCloudStorageActionState()

        val approverSetup = state.ownerState?.guardianSetup?.guardians ?: emptyList()
        val ownerApprover: Guardian.ProspectGuardian? = approverSetup.ownerApprovers()

        val entropy = (ownerApprover?.status as? GuardianStatus.OwnerAsApprover)?.entropy!!
        val deviceId = keyRepository.retrieveSavedDeviceId()

        val publicKey =
            Base58EncodedGuardianPublicKey(
                encryptedKey.decryptWithEntropy(
                    deviceKeyId = deviceId,
                    entropy = entropy
                ).toEncryptionKey().publicExternalRepresentation().value
            )

        state = state.copy(
            keyData = PlanSetupKeyData(
                encryptedPrivateKey = encryptedKey,
                publicKey = publicKey
            )
        )

        checkUserHasSavedKeyAndSubmittedPolicy()
    }

    private fun onKeyUploadFailed(exception: Exception?) {
        state = state.copy(
            createPolicySetupResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudUpload)
    }

    private fun onKeyDownloadFailed(exception: Exception?) {
        state = state.copy(
            createPolicySetupResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudDownload)
    }
    //endregion

    //region Reset functions
    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    private fun resetCloudStorageActionState() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            saveKeyToCloud = Resource.Uninitialized
        )
    }
    //endregion

    //region Extension Functions Mapping Approver Types

    private fun List<Guardian.ProspectGuardian>.ownerApprovers(): Guardian.ProspectGuardian? {
        return find { it.status is GuardianStatus.OwnerAsApprover || it.status is GuardianStatus.ImplicitlyOwner }
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
    //endregion
}