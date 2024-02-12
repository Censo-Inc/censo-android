package co.censo.shared.data.repository

import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiRequest
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiResponse
import co.censo.shared.data.model.CancelTakeoverApiResponse
import co.censo.shared.data.model.InitiateTakeoverApiResponse
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiRequest
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiResponse
import co.censo.shared.data.networking.ApiService

interface BeneficiaryRepository {
    suspend fun acceptBeneficiaryInvitation(
        inviteId: String,
        requestBody: AcceptBeneficiaryInvitationApiRequest
    ): Resource<AcceptBeneficiaryInvitationApiResponse>

    suspend fun submitBeneficiaryVerification(
        invitationId: String,
        apiRequest: SubmitBeneficiaryVerificationApiRequest
    ): Resource<SubmitBeneficiaryVerificationApiResponse>

    suspend fun initiateTakeover(): Resource<InitiateTakeoverApiResponse>
    suspend fun cancelTakeover(): Resource<CancelTakeoverApiResponse>
}

class BeneficiaryRepositoryImpl(
    private val apiService: ApiService
) : BeneficiaryRepository, BaseRepository() {
    override suspend fun acceptBeneficiaryInvitation(
        inviteId: String,
        requestBody: AcceptBeneficiaryInvitationApiRequest
    ): Resource<AcceptBeneficiaryInvitationApiResponse> {
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

    override suspend fun initiateTakeover(): Resource<InitiateTakeoverApiResponse> {
        return retrieveApiResource {
            apiService.initiateTakeover()
        }
    }

    override suspend fun cancelTakeover(): Resource<CancelTakeoverApiResponse> {
        return retrieveApiResource {
            apiService.cancelTakeover()
        }
    }
}


