package co.censo.shared.data.model

import Base58EncodedBeneficiaryPublicKey
import Base64EncodedData
import ParticipantId
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class BeneficiaryInvitationId(val value: String)
@Serializable
data class InviteBeneficiaryApiRequest(
    val label: String,
    val deviceEncryptedTotpSecret: Base64EncodedData,
)

@Serializable
data class InviteBeneficiaryApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class RemoveBeneficiaryApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class AcceptBeneficiaryInvitationApiRequest(
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class AcceptBeneficiaryInvitationApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob
)

@Serializable
data class SubmitBeneficiaryVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val beneficiaryPublicKey: Base58EncodedBeneficiaryPublicKey,
)

@Serializable
data class SubmitBeneficiaryVerificationApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class BeneficiaryEncryptedKey(
    val participantId: ParticipantId,
    val encryptedKey: Base64EncodedData,
)

@Serializable
data class ActivateBeneficiaryApiRequest(
    val keyConfirmationSignature: Base64EncodedData,
    val keyConfirmationTimeMillis: Long,
    val encryptedKeys: List<BeneficiaryEncryptedKey>,
)

@Serializable
data class ActivateBeneficiaryApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class RejectBeneficiaryVerificationApiResponse(
    val ownerState: OwnerState,
)