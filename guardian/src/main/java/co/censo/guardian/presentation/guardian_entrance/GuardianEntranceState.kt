package co.censo.guardian.presentation.guardian_entrance

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.GetGuardianStateApiResponse
import co.censo.shared.data.model.RegisterGuardianApiResponse
import okhttp3.ResponseBody

data class GuardianEntranceState(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = Base58EncodedDevicePublicKey(""),
    val intermediateKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(""),
    val verificationCode: String = "",
    val guardianStatus: GuardianStatus = GuardianStatus.UNINITIALIZED,
    val getGuardianResource: Resource<GetGuardianStateApiResponse> = Resource.Uninitialized,
    val registerGuardianResource: Resource<RegisterGuardianApiResponse> = Resource.Uninitialized,
    val acceptGuardianshipResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val declineGuardianshipResource: Resource<ResponseBody> = Resource.Uninitialized,
) {
    val isVerificationCodeValid = verificationCode.isNotEmpty() && verificationCode.length == VERIFICATION_CODE_LENGTH

    companion object {
        const val VERIFICATION_CODE_LENGTH = 6
    }
}

enum class GuardianStatus {
    UNINITIALIZED, DATA_MISSING, DECLINED, REGISTER_GUARDIAN,
    WAITING_FOR_CODE, WAITING_FOR_SHARD, SHARD_RECEIVED, COMPLETE
}

data class GuardianEntranceArgs(
    val participantId: ParticipantId = ParticipantId(""),
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey = Base58EncodedDevicePublicKey(""),
    val intermediateKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey("")
) {
    fun isDataMissing() = participantId.value.isEmpty() || ownerDevicePublicKey.value.isEmpty() || intermediateKey.value.isEmpty()
}
