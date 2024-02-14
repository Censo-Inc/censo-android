package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class InitiateTakeoverApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class CancelTakeoverApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class ApproveTakeoverInitiationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
)

@Serializable
data class ApproveTakeoverInitiationApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RejectTakeoverInitiationApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class StoreTakeoverTotpSecretApiRequest(
    val deviceEncryptedTotpSecret: Base64EncodedData,
)

@Serializable
data class StoreTakeoverTotpSecretApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class SubmitTakeoverTotpVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val beneficiaryPublicKey: Base58EncodedDevicePublicKey,
)

@Serializable
data class SubmitTakeoverTotpVerificationApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class ApproveTakeoverTotpVerificationApiRequest(
    val encryptedKey: Base64EncodedData,
)

@Serializable
data class ApproveTakeoverTotpVerificationApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RejectTakeoverTotpVerificationApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RetrieveTakeoverKeyApiRequest(
    val biometryData: Authentication.FacetecBiometry,
)

@Serializable
data class RetrieveTakeoverKeyApiResponse(
    val ownerState: OwnerState,
    val encryptedKey: Base64EncodedData,
    val scanResultBlob: String,
)

@Serializable
data class RetrieveTakeoverKeyWithPasswordApiRequest(
    val password: Authentication.Password,
)

@Serializable
data class RetrieveTakeoverKeyWithPasswordApiResponse(
    val ownerState: OwnerState,
    val encryptedKey: Base64EncodedData,
)

@Serializable
data class FinalizeTakeoverApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val password: Authentication.Password? = null,
)

@Serializable
data class FinalizeTakeoverApiResponse(
    val ownerState: OwnerState,
)