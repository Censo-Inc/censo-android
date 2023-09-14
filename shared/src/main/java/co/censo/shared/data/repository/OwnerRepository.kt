package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import GuardianProspect
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.ExternalDeviceKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.ConfirmShardReceiptApiRequest
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InviteGuardianApiRequest
import co.censo.shared.data.model.PolicyGuardian
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.log
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun createOwner(): Resource<ResponseBody>

    fun checkValidTimestamp(): Boolean
    fun saveValidTimestamp()
    suspend fun setupPolicy(threshold: Int, guardians: List<String>) : PolicySetupHelper
    suspend fun retrieveGuardianDeepLinks(guardians: List<PolicyGuardian.ProspectGuardian>, policyKey: Base58EncodedIntermediatePublicKey) : List<String>
    suspend fun createPolicy(setupHelper: PolicySetupHelper) : Resource<ResponseBody>
    suspend fun inviteGuardian(
        participantId: ParticipantId,
        intermediatePublicKey: Base58EncodedIntermediatePublicKey,
        guardian: PolicyGuardian.ProspectGuardian
    ): Resource<ResponseBody>


    fun checkCodeMatches(
        verificationCode: String,
        transportKey: Base58EncodedDevicePublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean

    fun encryptShardWithGuardianKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ) : ByteArray?

    suspend fun confirmShardReceipt(
        intermediatePublicKey: Base58EncodedIntermediatePublicKey,
        participantId: ParticipantId,
        encryptedShard: Base64EncodedData
    ) : Resource<ResponseBody>
}

class OwnerRepositoryImpl(
    private val apiService: ApiService,
    private val storage: Storage
) :
    OwnerRepository, BaseRepository() {

    companion object {
        const val GUARDIAN_URI = "guardian://guardian/"
    }

    override suspend fun retrieveUser(): Resource<GetUserApiResponse> {
        return retrieveApiResource { apiService.user() }
    }

    override suspend fun createOwner(): Resource<ResponseBody> {
        return retrieveApiResource { apiService.createUser() }
    }

    override fun checkValidTimestamp(): Boolean {
        val now = Clock.System.now()
        val cachedHeaders = storage.retrieveReadHeaders()
        return !(cachedHeaders == null || cachedHeaders.isExpired(now))
    }

    override fun saveValidTimestamp() {
        try {
            val cachedReadCallHeaders = InternalDeviceKey().createAuthHeaders(Clock.System.now())
            storage.saveReadHeaders(cachedReadCallHeaders)
        } catch (e: Exception) {
            //TODO: Log exception with raygun
        }
    }

    override suspend fun setupPolicy(threshold: Int, guardians: List<String>): PolicySetupHelper {

        val guardianProspect = guardians.map {
            GuardianProspect(
                label = it,
                participantId = generatePartitionId()
            )
        }

        val deviceKey = InternalDeviceKey()

        val policySetupHelper = PolicySetupHelper.create(
            threshold = threshold,
            guardians = guardianProspect,
            deviceKey = deviceKey.key
        ) {
            deviceKey.encrypt(it)
        }

        log(message = "Policy Setup Helper Created: $policySetupHelper")

        return policySetupHelper
    }

    override suspend fun createPolicy(setupHelper: PolicySetupHelper): Resource<ResponseBody> {
        val createPolicyApiRequest = CreatePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            guardiansToInvite = setupHelper.guardianInvites,
            threshold = setupHelper.threshold,
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun inviteGuardian(
        participantId: ParticipantId,
        intermediatePublicKey: Base58EncodedIntermediatePublicKey,
        guardian: PolicyGuardian.ProspectGuardian
    ): Resource<ResponseBody> {

        val deviceEncryptedPin = InternalDeviceKey().encrypt("123456".toByteArray(Charsets.UTF_8))

        return retrieveApiResource {
            apiService.inviteGuardian(
                intermediateKey = intermediatePublicKey.value,
                participantId = participantId.value,
                inviteGuardianApiRequest =
                    InviteGuardianApiRequest(
                        deviceEncryptedPin = Base64EncodedData(
                            Base64.getEncoder().encodeToString(deviceEncryptedPin)
                        )
                    )
            )
        }
    }
    override fun checkCodeMatches(
        verificationCode: String,
        transportKey: Base58EncodedDevicePublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ) =
        try {

            val guardianDevicePublicKey = transportKey.ecPublicKey

            val dataToSign =
                Guardian.createNonceAndCodeData(
                    time = timeMillis,
                    code = verificationCode
                )

            val externalDeviceKey = ExternalDeviceKey(guardianDevicePublicKey)

            externalDeviceKey.verify(
                signedData = dataToSign,
                signature = Base64.getDecoder().decode(signature.base64Encoded),
            )
        } catch (e: Exception) {
            false
        }

    override fun encryptShardWithGuardianKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ): ByteArray? {
        return try {
            val guardianDevicePublicKey = transportKey.ecPublicKey

            val decryptedShard = InternalDeviceKey().decrypt(
                Base64.getDecoder().decode(deviceEncryptedShard.base64Encoded)
            )

            ECIESManager.encryptMessage(
                dataToEncrypt = decryptedShard,
                publicKeyBytes = ECPublicKeyDecoder.extractUncompressedPublicKey(
                    guardianDevicePublicKey.encoded
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun confirmShardReceipt(
        intermediatePublicKey: Base58EncodedIntermediatePublicKey,
        participantId: ParticipantId,
        encryptedShard: Base64EncodedData
    ): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.confirmShardReceipt(
                intermediateKey = intermediatePublicKey.value,
                participantId = participantId.value,
                confirmShardReceiptApiRequest = ConfirmShardReceiptApiRequest(encryptedShard)
            )
        }
    }

    override suspend fun retrieveGuardianDeepLinks(
        guardians: List<PolicyGuardian.ProspectGuardian>,
        policyKey: Base58EncodedIntermediatePublicKey
    ): List<String> {
        val devicePublicKey = InternalDeviceKey().publicExternalRepresentation()

        //3. Create deep links from this: generateGuardianDeeplink
        return guardians.map {
            generateGuardianDeeplink(
                participantId = it.participantId.value,
                policyKey = policyKey.value,
                devicePublicKey = devicePublicKey.value
            )
        }
    }

    private fun generateGuardianDeeplink(
        participantId: String,
        policyKey: String,
        devicePublicKey: String
    ): String {
        return "$GUARDIAN_URI$policyKey/$devicePublicKey/$participantId"
    }
}