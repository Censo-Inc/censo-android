package co.censo.guardian.presentation.guardian_entrance

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetGuardianStateApiResponse

data class GuardianEntranceState(
    val participantId: ParticipantId = ParticipantId(""),
    val guardianStatus: GuardianStatus = GuardianStatus.LOGIN,
    val getGuardianResource: Resource<GetGuardianStateApiResponse> = Resource.Uninitialized,
)
enum class GuardianStatus {
    UNINITIALIZED, LOGIN, REGISTER, DISPLAY_QR_CODE_FOR_SCANNING, COMPLETE
}
