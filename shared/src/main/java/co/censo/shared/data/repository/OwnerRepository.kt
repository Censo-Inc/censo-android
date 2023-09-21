package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import GuardianProspect
import InvitationId
import ParticipantId
import co.censo.shared.BuildConfig
import co.censo.shared.SharedScreen.Companion.GUARDIAN_URI
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.ConfirmShardReceiptApiRequest
import co.censo.shared.data.model.CreateGuardianApiRequest
import co.censo.shared.data.model.CreateGuardianApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InviteGuardianApiRequest
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.JwtToken
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.projectLog
import com.auth0.android.jwt.JWT
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import okhttp3.ResponseBody
import java.util.Base64

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun createUser(jwtToken: String, idToken: String): Resource<ResponseBody>
    suspend fun setupPolicy(threshold: Int, guardians: List<String>) : PolicySetupHelper
    suspend fun createPolicy(
        setupHelper: PolicySetupHelper, biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<CreatePolicyApiResponse>
    suspend fun createGuardian(guardianName: String) : Resource<CreateGuardianApiResponse>
    suspend fun verifyToken(token: String) : String?
    suspend fun saveJWT(jwtToken: String)
    suspend fun retrieveJWT() : String
    suspend fun checkJWTValid(jwtToken: String) : Boolean
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
    private val storage: Storage
) : OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(): Resource<GetUserApiResponse> {
        return retrieveApiResource { apiService.user() }
    }

    override suspend fun createUser(authId: String, idToken: String) =
        retrieveApiResource {
            apiService.signIn(
                SignInApiRequest(
                    identityToken = IdentityToken(idToken),
                    jwtToken = JwtToken(authId)
                )
            )
        }

    override suspend fun setupPolicy(threshold: Int, guardians: List<String>): PolicySetupHelper {

        val guardianProspect = guardians.map {
            GuardianProspect(
                label = it,
                participantId = generatePartitionId()
            )
        }

        val deviceKey = InternalDeviceKey(storage.retrieveDeviceKeyId())

        val policySetupHelper = PolicySetupHelper.create(
            threshold = threshold,
            guardians = guardianProspect,
            deviceKey = deviceKey.retrieveKey()
        ) {
            deviceKey.encrypt(it)
        }

        projectLog(message = "Policy Setup Helper Created: $policySetupHelper")

        return policySetupHelper
    }

    override suspend fun createPolicy(
        setupHelper: PolicySetupHelper,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<CreatePolicyApiResponse> {
        val createPolicyApiRequest = CreatePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            threshold = setupHelper.threshold,
            guardians = emptyList(),

            biometryVerificationId = biometryVerificationId,
            biometryData = biometryData
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun createGuardian(guardianName: String): Resource<CreateGuardianApiResponse> {
        val createGuardianApiRequest = CreateGuardianApiRequest(name = guardianName)
        return retrieveApiResource { apiService.createGuardian(createGuardianApiRequest) }
    }
    override suspend fun verifyToken(token: String): String? {
        val verifier = GoogleIdTokenVerifier.Builder(
            NetHttpTransport(), GsonFactory()
        )
            .setAudience(BuildConfig.ONE_TAP_CLIENT_IDS.toList())
            .build()

        val verifiedIdToken: GoogleIdToken? = verifier.verify(token)

        return verifiedIdToken?.payload?.subject
    }

    override suspend fun saveJWT(jwtToken: String) {
        storage.saveJWT(jwtToken)
    }

    override suspend fun retrieveJWT() = storage.retrieveJWT()
    override suspend fun checkJWTValid(jwtToken: String): Boolean {
        return try {
            val jwtDecoded = JWT(jwtToken)
            //todo: See what we need to check on the token
            return true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun inviteGuardian(
        participantId: ParticipantId,
    ): Resource<InviteGuardianApiResponse> {

        val deviceEncryptedPin =
            InternalDeviceKey(storage.retrieveDeviceKeyId()).encrypt("123456".toByteArray(Charsets.UTF_8))

        return retrieveApiResource {
            apiService.inviteGuardian(
                participantId = participantId.value,
                inviteGuardianApiRequest =
                InviteGuardianApiRequest(
                    deviceEncryptedTotpSecret = Base64EncodedData(
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

            val decryptedShard = InternalDeviceKey(storage.retrieveDeviceKeyId()).decrypt(
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
}