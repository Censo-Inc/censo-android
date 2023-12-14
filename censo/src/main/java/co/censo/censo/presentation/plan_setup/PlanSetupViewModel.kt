package co.censo.censo.presentation.plan_setup

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.censo.presentation.Screen
import co.censo.censo.util.asExternalApprover
import co.censo.censo.util.asOwnerAsApprover
import co.censo.censo.util.confirmed
import co.censo.censo.util.externalApprovers
import co.censo.censo.util.notConfirmed
import co.censo.censo.util.ownerApprover
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

//TODO: Update docs

//TODO: Build towards being able to test the flow
// VMs split ---
// VMs contain only necesarry logic ----
// Clean up PlanFinalizationVM and determine a strong solution for the two VMs to communicate *****
// PlanSetupVM can communicate and trigger PlanFinalization methods via View
// PlanSetupScreen is cleaned up to handle two states/two VMs
/**
 *
 * Main Processes:
 *
 * Process 1a: Setup Approvers: (Primary and Alternate)
 *      Set the user nickname
 *          (edit user nickname)
 *      Activate Approver
 *      Create Setup Policy as we move forward
 *          Called multiple times as we are adding each approver
 *
 * Process 1b: Remove Approvers:
 *      Create Setup Policy with the owner as the only approver
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
 * Process 3: Complete access to set new plan
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
 *      If alternate approver: Start access to create new plan
 *
 * onFullyCompleted: Send user home. They have setup plan.
 *
 * onSaveAndFinishPlan: User done modifying the plan
 *      If we never confirmed the alternate approver, submit new policy, then initiate access
 *      If confirmed alternate approver, then initiate access
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
 * initiateAccess: Finalized the plan setup, and need to do access to re-shard and finalize plan.
 *      API call to initiate access. If that is a success, send user to Facetec.
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
            //TODO: Test approver confirmed functionality when alternate approver is added
            PlanSetupAction.ApproverConfirmed -> onApproverConfirmed()
            PlanSetupAction.EditApproverNickname -> onEditApproverNickname()
            PlanSetupAction.GoLiveWithApprover -> onGoLiveWithApprover()
            PlanSetupAction.InviteApprover -> onInviteAlternateApprover()
            PlanSetupAction.SaveApproverAndSavePolicy -> saveApproverAndSubmitPolicy()
            PlanSetupAction.EditApproverAndSavePolicy -> updateApproverNicknameAndSubmitPolicy()
        }
    }
    //endregion

    //region Lifecycle Methods
    fun onCreate(planSetupDirection: PlanSetupDirection) {
        state = state.copy(planSetupDirection = planSetupDirection)

        if (state.planSetupDirection == PlanSetupDirection.RemoveApprovers) {
            onRemoveApprovers()
        }
    }

    fun onResume() {
        if (state.planSetupDirection == PlanSetupDirection.AddApprovers) {
            // should be called on resume due to polling timers
            onAddApprovers()
        }
    }

    private fun onAddApprovers() {
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

    private fun onRemoveApprovers() {
        // create policy setup only with owner approver, save and exit
        val ownerAsApprover = state.ownerApprover?.asOwnerAsApprover() ?: createOwnerApprover()
        submitPolicySetup(
            threshold = state.planSetupDirection.threshold,
            policySetupApprovers = listOf(ownerAsApprover),
            nextAction = {
                //Next action is to trigger plan finalization
                triggerPlanFinalization()
            }
        )
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

        submitPolicySetup(
            threshold = state.planSetupDirection.threshold,
            policySetupApprovers = getUpdatedPolicySetupApproverList(ownerAsApprover),
            nextAction = {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive_4)
            }
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

        val updatedPolicySetupApprovers = when (state.approverType) {
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

        submitPolicySetup(
            threshold = state.planSetupDirection.threshold,
            policySetupApprovers = updatedPolicySetupApprovers,
            nextAction = {
                state = state.copy(planSetupUIState = PlanSetupUIState.ApproverGettingLive_4)
            }
        )
    }

    private fun onGoLiveWithApprover() {
        state = state.copy(planSetupUIState = PlanSetupUIState.ApproverActivation_5)
    }

    private fun onApproverConfirmed() {
        if (state.alternateApprover == null) {
            state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover_6)
        } else {
            triggerPlanFinalization()
        }
    }

    //TODO: Update the below method and slot into the updated screen
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
        val approverSetup = ownerState.policySetup?.approvers ?: emptyList()
        val externalApprovers = approverSetup.externalApprovers()
        val ownerApprover: Approver.ProspectApprover? = approverSetup.ownerApprover()
        val primaryApprover: Approver.ProspectApprover? = when {
            externalApprovers.isEmpty() -> null
            externalApprovers.size == 1 -> externalApprovers.first()
            else -> externalApprovers.confirmed().minBy { (it.status as ApproverStatus.Confirmed).confirmedAt }
        }
        val alternateApprover: Approver.ProspectApprover? = when {
            externalApprovers.isEmpty() -> null
            externalApprovers.size == 1 -> null
            else -> {
                val notConfirmed = externalApprovers.notConfirmed()
                when {
                    notConfirmed.isEmpty() -> externalApprovers.confirmed().maxByOrNull { (it.status as ApproverStatus.Confirmed).confirmedAt }!!
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
                    planSetupUIState = PlanSetupUIState.ApproverGettingLive_4
                )
            } else if (alternateApprover?.status is ApproverStatus.Confirmed) {
                triggerPlanFinalization()
            } else if (primaryApprover?.status is ApproverStatus.Confirmed) {
                state = state.copy(planSetupUIState = PlanSetupUIState.AddAlternateApprover_6)
            }
        }

        // verify approvers
        approverSetup.filter {
            it.status is ApproverStatus.VerificationSubmitted
        }.forEach {
            verifyApprover(
                it.participantId,
                it.status as ApproverStatus.VerificationSubmitted
            )
        }
    }

    private fun createOwnerApprover(): Approver.SetupApprover.OwnerAsApprover {
        val participantId = ParticipantId.generate()

        return Approver.SetupApprover.OwnerAsApprover(
            label = "Me",
            participantId = participantId,
        )
    }

    private fun getUpdatedPolicySetupApproverList(ownerApprover: Approver.SetupApprover.OwnerAsApprover): List<Approver.SetupApprover> =
        listOfNotNull(
            ownerApprover,
            state.primaryApprover?.asExternalApprover()
                ?: createExternalApprover(state.editedNickname),
            if (state.primaryApprover?.status is ApproverStatus.Confirmed) {
                state.alternateApprover?.asExternalApprover()
                    ?: createExternalApprover(state.editedNickname)
            } else {
                null
            }
        )

    //This needs to happen before we save any key information
    private fun submitPolicySetup(threshold: UInt, policySetupApprovers: List<Approver.SetupApprover>, nextAction: () -> Unit) {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.createPolicySetup(
                threshold = threshold,
                approvers = policySetupApprovers
            )

            if (response is Resource.Success) {
                updateOwnerState(response.data!!.ownerState)

                nextAction()
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    private fun createExternalApprover(nickname: String): Approver.SetupApprover.ExternalApprover {
        val participantId = ParticipantId.generate()
        val totpSecret = TotpGenerator.generateSecret()
        val encryptedTotpSecret = keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

        return Approver.SetupApprover.ExternalApprover(
            label = nickname,
            participantId = participantId,
            deviceEncryptedTotpSecret = encryptedTotpSecret
        )
    }

    private fun generateTimeCodes(approvers: List<Approver>): Map<ParticipantId, String> {

        return approvers.mapNotNull { approver ->
            when {
                approver is Approver.ProspectApprover && approver.status.resolveDeviceEncryptedTotpSecret() != null -> {
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

    private fun verifyApprover(
        participantId: ParticipantId,
        approverStatus: ApproverStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            encryptedTotpSecret = approverStatus.deviceEncryptedTotpSecret,
            transportKey = approverStatus.approverPublicKey,
            signature = approverStatus.signature,
            timeMillis = approverStatus.timeMillis
        )

        viewModelScope.launch {
            if (codeVerified) {

                val keyConfirmationTimeMillis = Clock.System.now().toEpochMilliseconds()

                val keyConfirmationMessage =
                    approverStatus.approverPublicKey.getBytes() + participantId.getBytes() + keyConfirmationTimeMillis.toString()
                        .toByteArray()
                val keyConfirmationSignature =
                    keyRepository.retrieveInternalDeviceKey().sign(keyConfirmationMessage)

                val confirmApprovershipResponse = ownerRepository.confirmApprovership(
                    participantId = participantId,
                    keyConfirmationSignature = keyConfirmationSignature,
                    keyConfirmationTimeMillis = keyConfirmationTimeMillis
                )

                if (confirmApprovershipResponse is Resource.Success) {
                    updateOwnerState(confirmApprovershipResponse.data!!.ownerState)
                }
            } else {
                val rejectVerificationResponse = ownerRepository.rejectVerification(participantId)

                if (rejectVerificationResponse is Resource.Success) {
                    updateOwnerState(rejectVerificationResponse.data!!.ownerState)
                }
            }
        }
    }

    fun triggerPlanFinalization() {
        state = state.copy(finalizePlanSetup = Resource.Success(Unit))
    }

    //endregion

    //region Reset functions
    fun resetFinalizePlanSetup() {
        state = state.copy(finalizePlanSetup = Resource.Uninitialized)
    }
    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun resetInitiateAccessResponse() {
        state = state.copy(initiateAccessResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicySetupResponse() {
        state = state.copy(createPolicySetupResponse = Resource.Uninitialized)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetVerifyKeyConfirmationSignature() {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Uninitialized)
    }

    fun resetRetrieveAccessShardsResponse() {
        state = state.copy(retrieveAccessShardsResponse = Resource.Uninitialized)
    }
    //endregion
}
