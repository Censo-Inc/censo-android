package co.censo.shared.data.repository

import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.AcceptGuardianshipApiRequest
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface GuardianRepository {
    fun signVerificationCode(verificationCode: String): Pair<Base64EncodedData, Long>
    suspend fun acceptGuardianship(
        invitationId: InvitationId,
        acceptGuardianshipApiRequest: AcceptGuardianshipApiRequest
    ): Resource<AcceptGuardianshipApiResponse>

    suspend fun declineGuardianship(
        invitationId: InvitationId,
    ): Resource<ResponseBody>

    fun saveInvitationId(invitationId: String)
    fun retrieveInvitationId(): String
}

class GuardianRepositoryImpl(
    private val apiService: ApiService,
    private val storage: Storage
) : GuardianRepository, BaseRepository() {

    override fun signVerificationCode(verificationCode: String): Pair<Base64EncodedData, Long> {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            verificationCode.toByteArray() + currentTimeInMillis.toString().toByteArray()
        val signature = InternalDeviceKey(storage.retrieveDeviceKeyId()).sign(dataToSign)
        val base64EncodedData = Base64EncodedData(Base64.getEncoder().encodeToString(signature))
        return Pair(base64EncodedData, currentTimeInMillis)
    }

    override suspend fun acceptGuardianship(
        invitationId: InvitationId,
        acceptGuardianshipApiRequest: AcceptGuardianshipApiRequest
    ): Resource<AcceptGuardianshipApiResponse> {
        return retrieveApiResource {
            apiService.acceptGuardianship(
                invitationId = invitationId.value,
                acceptGuardianshipApiRequest = acceptGuardianshipApiRequest
            )
        }
    }

    override suspend fun declineGuardianship(
        invitationId: InvitationId,
    ): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.declineGuardianship(
                invitationId = invitationId.value,
            )
        }
    }

    override fun saveInvitationId(invitationId: String) {
        storage.saveGuardianInvitationId(invitationId)
    }

    override fun retrieveInvitationId() = storage.retrieveGuardianInvitationId()
}