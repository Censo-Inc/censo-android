package co.censo.shared.data.model

import Base64EncodedData
import ParticipantId
import SeedPhraseId
import kotlinx.serialization.Serializable


@Serializable
data class ApproverContactInfo(
    val participantId: ParticipantId,
    val beneficiaryKeyEncryptedInfo: Base64EncodedData,
    val ownerApproverKeyEncryptedInfo: Base64EncodedData,
    val masterKeyEncryptedInfo: Base64EncodedData,
)

@Serializable
data class UpdateBeneficiaryApproverContactInfoApiRequest(
    val approverContacts: List<ApproverContactInfo>,
)

@Serializable
data class UpdateBeneficiaryApproverContactInfoApiResponse(
    val ownerState: OwnerState,
)

data class DecryptedApproverContactInfo(
    val participantId: ParticipantId,
    val label: String,
    val contactInfo: String,
)

data class DecryptedSeedPhraseNotes(
    val guid: SeedPhraseId,
    val label: String,
    val notes: String,
)