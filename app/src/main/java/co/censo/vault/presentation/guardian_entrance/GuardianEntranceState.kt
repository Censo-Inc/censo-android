package co.censo.vault.presentation.guardian_entrance

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import ParticipantId
import co.censo.vault.data.Resource
import okhttp3.ResponseBody

data class GuardianEntranceState(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = "",
    val intermediateKey: Base58EncodedPublicKey = "",
    val verificationCode: String = "",
    val guardianStatus: GuardianStatus = GuardianStatus.REGISTER_GUARDIAN,
    val registerGuardianResource: Resource<ResponseBody> = Resource.Uninitialized,
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized
)

enum class GuardianStatus {
    REGISTER_GUARDIAN, ENTER_VERIFICATION_CODE
}

data class GuardianEntranceArgs(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = "",
    val intermediateKey: Base58EncodedPublicKey = ""
) {
    fun isDataMissing() = participantId.value.isEmpty() || ownerDevicePublicKey.isEmpty() || intermediateKey.isEmpty()
}
