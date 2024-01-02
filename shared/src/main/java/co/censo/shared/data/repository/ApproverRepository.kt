package co.censo.shared.data.repository

import Base58EncodedApproverPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.AcceptApprovershipApiResponse
import co.censo.shared.data.model.ApproveAccessApiRequest
import co.censo.shared.data.model.ApproveAccessApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.LabelOwnerByApproverApiRequest
import co.censo.shared.data.model.RejectAccessApiResponse
import co.censo.shared.data.model.StoreAccessTotpSecretApiRequest
import co.censo.shared.data.model.StoreAccessTotpSecretApiResponse
import co.censo.shared.data.model.SubmitApproverVerificationApiRequest
import co.censo.shared.data.model.SubmitApproverVerificationApiResponse
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface ApproverRepository {
    suspend fun retrieveUser(): Resource<GetApproverUserApiResponse>
    suspend fun acceptApprovership(
        invitationId: InvitationId,
    ): Resource<AcceptApprovershipApiResponse>

    suspend fun declineApprovership(
        invitationId: InvitationId,
    ): Resource<ResponseBody>

    fun saveInvitationId(invitationId: String)
    fun retrieveInvitationId(): String
    fun clearInvitationId()
    suspend fun submitApproverVerification(
        invitationId: String,
        submitApproverVerificationRequest: SubmitApproverVerificationApiRequest
    ): Resource<SubmitApproverVerificationApiResponse>

    fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitApproverVerificationApiRequest

    fun saveParticipantId(participantId: String)
    fun retrieveParticipantId(): String
    fun clearParticipantId()
    fun saveApprovalId(approvalId: String)
    fun retrieveApprovalId(): String
    fun clearApprovalId()

    fun checkTotpMatches(
        encryptedTotpSecret: Base64EncodedData,
        ownerPublicKey: Base58EncodedPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean

    suspend fun storeAccessTotpSecret(
        approvalId: String,
        encryptedTotpSecret: Base64EncodedData
    ) : Resource<StoreAccessTotpSecretApiResponse>

    suspend fun approveAccess(
        approvalId: String,
        encryptedShard: Base64EncodedData
    ) : Resource<ApproveAccessApiResponse>
    suspend fun rejectAccess(approvalId: String) : Resource<RejectAccessApiResponse>

    suspend fun deleteUser(): Resource<Unit>
    suspend fun signUserOut()
    suspend fun labelOwner(participantId: String, label: String) : Resource<GetApproverUserApiResponse>
}

class ApproverRepositoryImpl(
    private val apiService: ApiService,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences,
    private val keyRepository: KeyRepository
) : ApproverRepository, BaseRepository() {

    override suspend fun retrieveUser(): Resource<GetApproverUserApiResponse> {
        return retrieveApiResource { apiService.approverUser() }
    }

    override fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitApproverVerificationApiRequest {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            verificationCode.generateVerificationCodeSignData(currentTimeInMillis)
        val signature = encryptionKey.sign(dataToSign)
        val base64EncodedData = Base64EncodedData(Base64.getEncoder().encodeToString(signature))

        return SubmitApproverVerificationApiRequest(
            signature = base64EncodedData,
            timeMillis = currentTimeInMillis,
            approverPublicKey = Base58EncodedApproverPublicKey(
                encryptionKey.publicExternalRepresentation().value
            )
        )
    }

    override suspend fun acceptApprovership(
        invitationId: InvitationId,
    ): Resource<AcceptApprovershipApiResponse> {
        return retrieveApiResource {
            apiService.acceptApprovership(
                invitationId = invitationId.value,
            )
        }
    }

    //TODO: DEead
    override suspend fun declineApprovership(
        invitationId: InvitationId,
    ): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.declineApprovership(
                invitationId = invitationId.value,
            )
        }
    }

    override fun saveInvitationId(invitationId: String) {
        secureStorage.saveApproverInvitationId(invitationId)
    }

    override fun retrieveInvitationId() = secureStorage.retrieveApproverInvitationId()
    override fun clearInvitationId() = secureStorage.clearApproverInvitationId()

    override suspend fun submitApproverVerification(
        invitationId: String,
        submitApproverVerificationRequest: SubmitApproverVerificationApiRequest
    ): Resource<SubmitApproverVerificationApiResponse> {
        return retrieveApiResource {
            apiService.submitApproverVerification(
                invitationId = invitationId,
                submitApproverVerificationApiRequest = submitApproverVerificationRequest
            )
        }
    }

    override fun saveParticipantId(participantId: String) {
        secureStorage.saveApproverParticipantId(participantId)
    }

    override fun retrieveParticipantId(): String =
        secureStorage.retrieveApproverParticipantId()

    override fun clearParticipantId() = secureStorage.clearApproverParticipantId()
    override fun saveApprovalId(approvalId: String) {
        secureStorage.saveApprovalId(approvalId)
    }
    override fun retrieveApprovalId() = secureStorage.retrieveApprovalId()
    override fun clearApprovalId() = secureStorage.clearApprovalId()

    override suspend fun storeAccessTotpSecret(
        approvalId: String,
        encryptedTotpSecret: Base64EncodedData
    ) : Resource<StoreAccessTotpSecretApiResponse> {
        return retrieveApiResource {
            apiService.storeAccessTotpSecret(
                approvalId,
                StoreAccessTotpSecretApiRequest(deviceEncryptedTotpSecret = encryptedTotpSecret)
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
            e.sendError(CrashReportingUtil.TotpVerification)
            false
        }

    override suspend fun approveAccess(
        approvalId: String,
        encryptedShard: Base64EncodedData
    ) : Resource<ApproveAccessApiResponse> {
        return retrieveApiResource {
            apiService.approveAccess(
                approvalId,
                ApproveAccessApiRequest(encryptedShard)
            )
        }
    }

    override suspend fun rejectAccess(approvalId: String) : Resource<RejectAccessApiResponse> {
        return retrieveApiResource {
            apiService.rejectAccess(approvalId)
        }
    }

    override suspend fun signUserOut() {
        authUtil.signOut()
        secureStorage.clearJWT()
    }

    override suspend fun deleteUser(): Resource<Unit> {
        val response = retrieveApiResource { apiService.deleteUser() }

        if (response is Resource.Success) {
            try {
                keyRepository.deleteDeviceKeyIfPresent(secureStorage.retrieveDeviceKeyId())
                secureStorage.clearDeviceKeyId()
                signUserOut()
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.DeleteUser)
            }
        }

        return response
    }

    override suspend fun labelOwner(participantId: String, label: String): Resource<GetApproverUserApiResponse> {
        return retrieveApiResource {
            apiService.labelOwnerByApprover(participantId, LabelOwnerByApproverApiRequest(label))
        }
    }
}