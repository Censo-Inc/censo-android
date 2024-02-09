package co.censo.shared.data.repository

import Base58EncodedBeneficiaryPublicKey
import Base64EncodedData
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiRequest
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiResponse
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiRequest
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiResponse
import co.censo.shared.data.networking.ApiService
import kotlinx.datetime.Clock
import org.bouncycastle.util.encoders.Base64

interface BeneficiaryRepository {
    suspend fun acceptBeneficiaryInvitation(
        inviteId: String,
        requestBody: AcceptBeneficiaryInvitationApiRequest
    ): Resource<AcceptBeneficiaryInvitationApiResponse>
    suspend fun submitBeneficiaryVerification(
        invitationId: String,
        apiRequest: SubmitBeneficiaryVerificationApiRequest
    ): Resource<SubmitBeneficiaryVerificationApiResponse>
}

class BeneficiaryRepositoryImpl(
    private val apiService: ApiService
) : BeneficiaryRepository, BaseRepository() {
    override suspend fun acceptBeneficiaryInvitation(inviteId: String, requestBody: AcceptBeneficiaryInvitationApiRequest): Resource<AcceptBeneficiaryInvitationApiResponse> {
        return retrieveApiResource {
            apiService.acceptBeneficiaryInvitation(
                requestBody = requestBody,
                invitationId = inviteId
            )
        }
    }

    override suspend fun submitBeneficiaryVerification(
        invitationId: String,
        apiRequest: SubmitBeneficiaryVerificationApiRequest
    ): Resource<SubmitBeneficiaryVerificationApiResponse> {
        return retrieveApiResource {
            apiService.submitBeneficiaryVerification(
                invitationId = invitationId,
                requestBody = apiRequest
            )
        }
    }
}


