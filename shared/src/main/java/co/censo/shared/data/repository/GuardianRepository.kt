package co.censo.shared.data.repository

import Base58EncodedGuardianPublicKey
import Base58EncodedPrivateKey
import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.SubmitGuardianVerificationApiRequest
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface GuardianRepository {
    suspend fun acceptGuardianship(
        invitationId: InvitationId,
    ): Resource<AcceptGuardianshipApiResponse>

    suspend fun declineGuardianship(
        invitationId: InvitationId,
    ): Resource<ResponseBody>

    fun saveInvitationId(invitationId: String)
    fun retrieveInvitationId(): String
    suspend fun submitGuardianVerification(
        invitationId: String,
        submitGuardianVerificationRequest: SubmitGuardianVerificationApiRequest
    ): Resource<SubmitGuardianVerificationApiResponse>

    suspend fun userHasKeySavedInCloud(): Boolean
    suspend fun saveKeyInCloud(key: Base58EncodedPrivateKey)
    suspend fun retrieveKeyFromCloud(): Base58EncodedPrivateKey
    fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitGuardianVerificationApiRequest
}

class GuardianRepositoryImpl(
    private val apiService: ApiService, private val storage: Storage
) : GuardianRepository, BaseRepository() {

    override fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitGuardianVerificationApiRequest {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            verificationCode.toByteArray() + currentTimeInMillis.toString().toByteArray()
        val signature = encryptionKey.sign(dataToSign)
        val base64EncodedData = Base64EncodedData(Base64.getEncoder().encodeToString(signature))

        return SubmitGuardianVerificationApiRequest(
            signature = base64EncodedData,
            timeMillis = currentTimeInMillis,
            guardianPublicKey = Base58EncodedGuardianPublicKey(
                encryptionKey.publicExternalRepresentation().value
            )
        )
    }

    override suspend fun acceptGuardianship(
        invitationId: InvitationId,
    ): Resource<AcceptGuardianshipApiResponse> {
        return retrieveApiResource {
            apiService.acceptGuardianship(
                invitationId = invitationId.value,
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
    override suspend fun submitGuardianVerification(
        invitationId: String,
        submitGuardianVerificationRequest: SubmitGuardianVerificationApiRequest
    ): Resource<SubmitGuardianVerificationApiResponse> {
        return retrieveApiResource {
            apiService.submitGuardianVerification(
                invitationId = invitationId,
                submitGuardianVerificationApiRequest = submitGuardianVerificationRequest
            )
        }
    }

    override suspend fun userHasKeySavedInCloud(): Boolean {
        return storage.retrievePrivateKey().isNotEmpty()
    }

    override suspend fun saveKeyInCloud(key: Base58EncodedPrivateKey) {
        storage.savePrivateKey(key.value)
    }

    override suspend fun retrieveKeyFromCloud(): Base58EncodedPrivateKey {
        return Base58EncodedPrivateKey(storage.retrievePrivateKey())
    }
}