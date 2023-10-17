package co.censo.shared.data.repository

import Base58EncodedGuardianPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ApproveRecoveryApiRequest
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.RejectRecoveryApiResponse
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiRequest
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiResponse
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

    fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitGuardianVerificationApiRequest

    fun saveParticipantId(participantId: String)
    fun retrieveParticipantId(): String

    suspend fun storeRecoveryTotpSecret(
        participantId: String,
        encryptedTotpSecret: Base64EncodedData
    ): Resource<StoreRecoveryTotpSecretApiResponse>

    fun checkTotpMatches(
        totp: String,
        ownerPublicKey: Base58EncodedPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean

    suspend fun approveRecovery(
        participantId: ParticipantId,
        encryptedShard: Base64EncodedData,
    ): Resource<ApproveRecoveryApiResponse>

    suspend fun rejectRecovery(
        participantId: ParticipantId
    ) : Resource<RejectRecoveryApiResponse>
}

class GuardianRepositoryImpl(
    private val apiService: ApiService,
    private val storage: Storage
) : GuardianRepository, BaseRepository() {

    override fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitGuardianVerificationApiRequest {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            verificationCode.generateVerificationCodeSignData(currentTimeInMillis)
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

    override fun saveParticipantId(participantId: String) {
        storage.saveGuardianParticipantId(participantId)
    }

    override fun retrieveParticipantId(): String =
        storage.retrieveGuardianParticipantId()

    override suspend fun storeRecoveryTotpSecret(
        participantId: String,
        encryptedTotpSecret: Base64EncodedData
    ): Resource<StoreRecoveryTotpSecretApiResponse> {
        return retrieveApiResource {
            apiService.storeRecoveryTotpSecret(
                participantId,
                StoreRecoveryTotpSecretApiRequest(deviceEncryptedTotpSecret = encryptedTotpSecret)
            )
        }
    }

    override fun checkTotpMatches(
        totp: String,
        ownerPublicKey: Base58EncodedPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean =
        try {
            val dataToSign = totp.generateVerificationCodeSignData(timeMillis)
            ExternalEncryptionKey.generateFromPublicKeyBase58(ownerPublicKey).verify(
                signedData = dataToSign,
                signature = Base64.getDecoder().decode(signature.base64Encoded),
            )
        } catch (e: Exception) {
            false
        }

    override suspend fun approveRecovery(
        participantId: ParticipantId,
        encryptedShard: Base64EncodedData
    ): Resource<ApproveRecoveryApiResponse> {
        return retrieveApiResource {
            apiService.approveRecovery(participantId.value, ApproveRecoveryApiRequest(encryptedShard))
        }
    }

    override suspend fun rejectRecovery(participantId: ParticipantId): Resource<RejectRecoveryApiResponse> {
        return retrieveApiResource {
            apiService.rejectRecovery(participantId.value)
        }
    }
}