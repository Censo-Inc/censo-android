package co.censo.vault.presentation.guardian_invitation

import co.censo.vault.data.Resource
import co.censo.vault.data.model.Guardian
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT

data class GuardianInvitationState(
    val threshold: Int = 0,
    val guardians: List<Guardian> = emptyList(),
    val guardianDeepLinks: List<String> = emptyList(),
    val guardianInviteStatus: GuardianInvitationStatus = GuardianInvitationStatus.ADD_GUARDIANS,
) {
    val canContinueOnboarding = (guardians.size >= MIN_GUARDIAN_LIMIT && threshold > 0)
}

enum class GuardianInvitationStatus {
    ADD_GUARDIANS, INVITE_GUARDIANS
}