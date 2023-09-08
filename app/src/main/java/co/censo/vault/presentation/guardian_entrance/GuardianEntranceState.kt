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
    val guardianStatus: GuardianStatus = GuardianStatus.DATA_MISSING,
    val retrieveGuardianResource: Resource<GuardianStatus> = Resource.Uninitialized,
    val registerGuardianResource: Resource<ResponseBody> = Resource.Uninitialized,
    val acceptGuardianshipResource: Resource<ResponseBody> = Resource.Uninitialized,
    val declineGuardianshipResource: Resource<ResponseBody> = Resource.Uninitialized,
    val bioPromptTrigger: Resource<BiometryAuthReason> = Resource.Uninitialized
) {
    val isVerificationCodeValid = verificationCode.isNotEmpty() && verificationCode.length == VERIFICATION_CODE_LENGTH

    companion object {
        const val VERIFICATION_CODE_LENGTH = 6
    }
}

enum class GuardianStatus {
    DATA_MISSING, DECLINED, REGISTER_GUARDIAN, WAITING_FOR_CODE, WAITING_FOR_SHARD, SHARD_RECEIVED, COMPLETE
}

enum class BiometryAuthReason {
    REGISTER_GUARDIAN, SUBMIT_VERIFICATION_CODE
}

data class GuardianEntranceArgs(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = Base58EncodedDevicePublicKey(""),
    val intermediateKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey("")
) {
    fun isDataMissing() = participantId.value.isEmpty() || ownerDevicePublicKey.value.isEmpty() || intermediateKey.value.isEmpty()
}
