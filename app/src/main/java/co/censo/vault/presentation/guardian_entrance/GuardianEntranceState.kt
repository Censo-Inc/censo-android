package co.censo.vault.presentation.guardian_entrance

import Base58EncodedPublicKey
import co.censo.vault.data.Resource
import okhttp3.ResponseBody

data class GuardianEntranceState(
    val participantId: String = "",
    val ownerDevicePublicKey: String = "",
    val intermediateKey: Base58EncodedPublicKey = "",
    val registerGuardianResource: Resource<ResponseBody> = Resource.Uninitialized
)

data class GuardianEntranceArgs(
    val participantId: String = "",
    val ownerDevicePublicKey: String = "",
    val intermediateKey: Base58EncodedPublicKey = ""
) {
    fun isDataMissing() = participantId.isEmpty() || ownerDevicePublicKey.isEmpty() || intermediateKey.isEmpty()
}
