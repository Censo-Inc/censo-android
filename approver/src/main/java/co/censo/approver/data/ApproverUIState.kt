package co.censo.approver.data

sealed interface ApproverUIState

sealed class ApproverOnboardingUIState : ApproverUIState {
    object NeedsToSaveKey : ApproverOnboardingUIState()

    object NeedsToEnterCode : ApproverOnboardingUIState()

    object WaitingForConfirmation : ApproverOnboardingUIState()

    object CodeRejected : ApproverOnboardingUIState()

    object Complete : ApproverOnboardingUIState()
}

sealed class ApproverAccessUIState : ApproverUIState {

    object AccessRequested : ApproverAccessUIState()

    object WaitingForToTPFromOwner : ApproverAccessUIState()

    object VerifyingToTPFromOwner : ApproverAccessUIState()

    object Complete : ApproverAccessUIState()
}