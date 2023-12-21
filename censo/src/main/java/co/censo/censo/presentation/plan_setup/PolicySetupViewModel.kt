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
import co.censo.shared.util.NavigationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

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
 * triggerFinalizePlanSetup: Called after both approvers have been confirmed
 *      or if the user has finished approver setup but left before plan finalization occurred
 */


@HiltViewModel
class PolicySetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer,
) : ViewModel() {
    var state by mutableStateOf(PolicySetupState())
        private set

    //region Events
    fun receivePlanAction(action: PolicySetupScreenAction) {
        when (action) {
            //Back
            PolicySetupScreenAction.BackClicked -> onBackClicked()

            //Retry
            PolicySetupScreenAction.Retry -> retrieveOwnerState(silent = false)

            //Nickname or Approver Actions
            is PolicySetupScreenAction.ApproverNicknameChanged ->
                onApproverNicknameChanged(action.name)
            PolicySetupScreenAction.ApproverConfirmed -> onApproverConfirmed()
            PolicySetupScreenAction.EditApproverNickname -> onEditApproverNickname()
            PolicySetupScreenAction.GoLiveWithApprover -> onGoLiveWithApprover()
            PolicySetupScreenAction.SaveApproverAndSavePolicy -> saveApproverAndSubmitPolicy()
            PolicySetupScreenAction.EditApproverAndSavePolicy -> updateApproverNicknameAndSubmitPolicy()
        }
    }
    //endregion

    //region Lifecycle Methods
    fun onCreate(policySetupAction: PolicySetupAction) {
        state = state.copy(policySetupAction = policySetupAction)

        if (state.policySetupAction == PolicySetupAction.RemoveApprovers) {
            onRemoveApprovers()
        }
    }

    fun onResume() {
        if (state.policySetupAction == PolicySetupAction.AddApprovers) {
            // should be called on resume due to polling timers
            onAddApprovers()
        }
    }

    private fun onAddApprovers() {
        if (state.policySetupUIState == PolicySetupUIState.Initial_1) {
            state = state.copy(policySetupUIState = PolicySetupUIState.ApproverNickname_2)
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
            threshold = state.policySetupAction.threshold,
            policySetupApprovers = listOf(ownerAsApprover),
            nextAction = {
                //Next action is to trigger plan finalization
                triggerReplacePolicy()
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
            PolicySetupUIState.EditApproverNickname_3 to PolicySetupUIState.ApproverActivation_5,
            PolicySetupUIState.ApproverActivation_5 to PolicySetupUIState.ApproverGettingLive_4,
        ).toMap()

        when (state.backArrowType) {
            PolicySetupState.BackIconType.None -> {}

            PolicySetupState.BackIconType.Back -> {
                state = state.copy(
                    policySetupUIState = backIconNavigation[state.policySetupUIState] ?: state.policySetupUIState
                )
            }

            PolicySetupState.BackIconType.Exit -> {
                state = state.copy(
                    navigationResource = Resource.Success(
                        NavigationData(
                            route = Screen.OwnerVaultScreen.route,
                            popSelfFromBackStack = true
                        )
                    )
                )
            }
        }
    }

    private fun onInviteAlternateApprover() {
        state = if (state.alternateApprover != null) {
            // skip name entry of alternate approver if it is already set
            state.copy(
                policySetupUIState = PolicySetupUIState.ApproverGettingLive_4
            )
        } else {
            state.copy(
                editedNickname = "",
                policySetupUIState = PolicySetupUIState.ApproverNickname_2
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
            threshold = state.policySetupAction.threshold,
            policySetupApprovers = getUpdatedPolicySetupApproverList(ownerAsApprover),
            nextAction = {
                state = state.copy(policySetupUIState = PolicySetupUIState.ApproverGettingLive_4)
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
            policySetupUIState = PolicySetupUIState.EditApproverNickname_3
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
            threshold = state.policySetupAction.threshold,
            policySetupApprovers = updatedPolicySetupApprovers,
            nextAction = {
                state = state.copy(policySetupUIState = PolicySetupUIState.ApproverGettingLive_4)
            }
        )
    }

    private fun onGoLiveWithApprover() {
        state = state.copy(policySetupUIState = PolicySetupUIState.ApproverActivation_5)
    }

    private fun onApproverConfirmed() {
        if (state.alternateApprover == null) {
            onInviteAlternateApprover()
        } else {
            triggerReplacePolicy()
        }
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
        if (overwriteUIState && state.policySetupUIState == PolicySetupUIState.ApproverNickname_2) {
            if (externalApprovers.notConfirmed().isNotEmpty()) {
                state = state.copy(
                    editedNickname = when (state.approverType) {
                        ApproverType.Primary -> state.primaryApprover?.label
                        ApproverType.Alternate -> state.alternateApprover?.label
                    } ?: "",
                    policySetupUIState = PolicySetupUIState.ApproverGettingLive_4
                )
            } else if (alternateApprover?.status is ApproverStatus.Confirmed) {
                triggerReplacePolicy()
            } else if (primaryApprover?.status is ApproverStatus.Confirmed) {
                onInviteAlternateApprover()
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

    private fun triggerReplacePolicy() {
        state = state.copy(replacePolicy = Resource.Success(Unit), policySetupUIState = PolicySetupUIState.Uninitialized_0)
    }
    //endregion

    //region Reset functions
    fun resetReplacePolicy() {
        state = state.copy(replacePolicy = Resource.Uninitialized)
    }
    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun resetCreatePolicySetupResponse() {
        state = state.copy(createPolicySetupResponse = Resource.Uninitialized)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }
    //endregion
}
