package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UnlockApiRequest(
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class UnlockApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class LockApiResponse(
    val ownerState: OwnerState,
)
