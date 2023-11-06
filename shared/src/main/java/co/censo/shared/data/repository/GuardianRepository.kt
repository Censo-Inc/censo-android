package co.censo.shared.data.repository

import Base58EncodedGuardianPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
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
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.sendError
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
    fun clearInvitationId()
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
    fun clearParticipantId()

    suspend fun storeRecoveryTotpSecret(
        participantId: String,
        encryptedTotpSecret: Base64EncodedData
    ): Resource<StoreRecoveryTotpSecretApiResponse>

    fun checkTotpMatches(
        encryptedTotpSecret: Base64EncodedData,
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
    private val secureStorage: SecurePreferences,
    private val keyRepository: KeyRepository
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
        secureStorage.saveGuardianInvitationId(invitationId)
    }

    override fun retrieveInvitationId() = secureStorage.retrieveGuardianInvitationId()
    override fun clearInvitationId() = secureStorage.clearGuardianInvitationId()

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
        secureStorage.saveGuardianParticipantId(participantId)
    }

    override fun retrieveParticipantId(): String =
        secureStorage.retrieveGuardianParticipantId()

    override fun clearParticipantId() = secureStorage.clearGuardianParticipantId()

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
        encryptedTotpSecret: Base64EncodedData,
        ownerPublicKey: Base58EncodedPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean =
        try {
            val externalDeviceKey =
                ExternalEncryptionKey.generateFromPublicKeyBase58(ownerPublicKey)

            val timeSeconds = timeMillis / 1000
            val secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes))
            listOf(
                timeSeconds,
                timeSeconds - TotpGenerator.CODE_EXPIRATION,
                timeSeconds + TotpGenerator.CODE_EXPIRATION
            ).any { ts ->
                val code = TotpGenerator.generateCode(
                    secret = secret,
                    counter = ts.div(TotpGenerator.CODE_EXPIRATION)
                )

                val dataToSign = code.generateVerificationCodeSignData(timeMillis)
                externalDeviceKey.verify(
                    signedData = dataToSign,
                    signature = Base64.getDecoder().decode(signature.base64Encoded),
                )
            }
        } catch (e: Exception) {
            e.sendError("TotpVerification")
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