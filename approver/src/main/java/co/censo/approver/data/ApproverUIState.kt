package co.censo.approver.data

sealed interface ApproverUIState

sealed class ApproverAccessUIState : ApproverUIState {

    object AccessRequested : ApproverAccessUIState()

    object WaitingForToTPFromOwner : ApproverAccessUIState()

    object VerifyingToTPFromOwner : ApproverAccessUIState()

    object Complete : ApproverAccessUIState()
}