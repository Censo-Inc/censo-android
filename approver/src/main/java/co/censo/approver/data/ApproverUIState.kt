package co.censo.approver.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApproverUIState

@Serializable
sealed class ApproverOnboardingUIState : ApproverUIState {
    @Serializable
    object UserNeedsPasteInvitationLink : ApproverOnboardingUIState()

    @Serializable
    object NeedsToEnterCode : ApproverOnboardingUIState()

    @Serializable
    object WaitingForConfirmation : ApproverOnboardingUIState()

    @Serializable
    object CodeRejected : ApproverOnboardingUIState()

    @Serializable
    object Complete : ApproverOnboardingUIState()
}

@Serializable
sealed class ApproverAccessUIState : ApproverUIState {

    @Serializable
    object UserNeedsPasteRecoveryLink : ApproverAccessUIState()

    @Serializable
    object AccessRequested : ApproverAccessUIState()

    @Serializable
    object WaitingForToTPFromOwner : ApproverAccessUIState()

    @Serializable
    object VerifyingToTPFromOwner : ApproverAccessUIState()

    @Serializable
    object AccessApproved : ApproverAccessUIState()

    @Serializable
    object Complete : ApproverAccessUIState()
}