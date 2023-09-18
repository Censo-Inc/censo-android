package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import GuardianProspect
import InvitationId
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.ExternalDeviceKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.ConfirmShardReceiptApiRequest
import co.censo.shared.data.model.CreateGuardianApiRequest
import co.censo.shared.data.model.CreateGuardianApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InviteGuardianApiRequest
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.log
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.Base64

interface OwnerRepository {

    suspend fun retrieveUser(getUserApiResponse: GetUserApiResponse? = null): Resource<GetUserApiResponse>
    suspend fun createOwner(authId: String): Resource<ResponseBody>

    suspend fun setupPolicy(threshold: Int, guardians: List<String>) : PolicySetupHelper
    suspend fun createPolicy(setupHelper: PolicySetupHelper) : Resource<ResponseBody>
    suspend fun createGuardian(guardianName: String, mockCreatedGuardians: List<Guardian.ProspectGuardian>) : Resource<CreateGuardianApiResponse>
    suspend fun inviteGuardian(
        participantId: ParticipantId,
    ): Resource<InviteGuardianApiResponse>
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
) : OwnerRepository, BaseRepository() {
    companion object {
        const val GUARDIAN_URI = "guardian://guardian/"
    }

    override suspend fun retrieveUser(getUserApiResponse: GetUserApiResponse?): Resource<GetUserApiResponse> {

        return Resource.Success(getUserApiResponse)
        return retrieveApiResource { apiService.user() }
    }

    override suspend fun createOwner(authId: String): Resource<ResponseBody> {
        return Resource.Success("".toResponseBody())
        return retrieveApiResource { apiService.createUser() }
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
            threshold = setupHelper.threshold,
            guardians = emptyList()
        )

        return Resource.Success("".toResponseBody())
        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun createGuardian(guardianName: String, mockCreatedGuardians: List<Guardian.ProspectGuardian>): Resource<CreateGuardianApiResponse> {
        val createGuardianApiRequest = CreateGuardianApiRequest(name = guardianName)

        return Resource.Success(
            data = CreateGuardianApiResponse(
                ownerState = OwnerState.GuardianSetup(
                    guardians = mockCreatedGuardians
                )
            )
        )

        return retrieveApiResource { apiService.createGuardian(createGuardianApiRequest) }
    }

    override suspend fun inviteGuardian(
        participantId: ParticipantId,
    ): Resource<InviteGuardianApiResponse> {

        val deviceEncryptedPin = InternalDeviceKey().encrypt("123456".toByteArray(Charsets.UTF_8))

        return retrieveApiResource {
            apiService.inviteGuardian(
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

    private fun generateGuardianDeeplink(
        invitationId: InvitationId,
    ): String {
        return "$GUARDIAN_URI${invitationId.value}"
    }
}