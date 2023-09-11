package co.censo.vault.presentation.guardian_entrance

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import ParticipantId
import co.censo.vault.data.Resource
import okhttp3.ResponseBody

data class GuardianEntranceState(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = Base58EncodedDevicePublicKey(""),
    val intermediateKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(""),
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
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = Base58EncodedDevicePublicKey(""),
    val intermediateKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey("")
) {
    fun isDataMissing() = participantId.value.isEmpty() || ownerDevicePublicKey.value.isEmpty() || intermediateKey.value.isEmpty()
}
