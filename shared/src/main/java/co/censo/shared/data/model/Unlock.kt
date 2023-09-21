package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UnlockVaultApiRequest(
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class UnlockVaultApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class LockVaultApiResponse(
    val ownerState: OwnerState,
)
