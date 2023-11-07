package co.censo.approver.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApproverUIState

@Serializable
sealed class ApproverOnboardingUIState : ApproverUIState {

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
    object AccessRequested : ApproverAccessUIState()

    @Serializable
    object WaitingForToTPFromOwner : ApproverAccessUIState()

    @Serializable
    object VerifyingToTPFromOwner : ApproverAccessUIState()
}