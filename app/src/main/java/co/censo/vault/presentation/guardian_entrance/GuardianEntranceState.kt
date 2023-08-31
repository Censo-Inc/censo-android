package co.censo.vault.presentation.guardian_entrance

import co.censo.vault.data.Resource
import okhttp3.ResponseBody

data class GuardianEntranceState(
    val participantId: String = "",
    val ownerDevicePublicKey: String = "",
    val policyKey: String = "",
    val registerGuardianResource: Resource<ResponseBody> = Resource.Uninitialized
)

data class GuardianEntranceArgs(
    val participantId: String = "",
    val ownerDevicePublicKey: String = "",
    val policyKey: String = ""
) {
    fun isDataMissing() = participantId.isEmpty() || ownerDevicePublicKey.isEmpty() || policyKey.isEmpty()
}
