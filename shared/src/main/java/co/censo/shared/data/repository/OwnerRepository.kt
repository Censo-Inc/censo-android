package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedGuardianPublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import ParticipantId
import VaultSecretId
import co.censo.shared.BuildConfig
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.ORDER
import co.censo.shared.data.cryptography.Point
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.SecretSharerUtils
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.sha256
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.ConfirmGuardianshipApiRequest
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiRequest
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateRecoveryApiRequest
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.JwtToken
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RejectGuardianVerificationApiResponse
import co.censo.shared.data.model.RetrieveRecoveryShardsApiRequest
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import co.censo.shared.data.model.SecurityPlanData
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.StoreSecretApiRequest
import co.censo.shared.data.model.StoreSecretApiResponse
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.projectLog
import com.auth0.android.jwt.JWT
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.math.BigInteger
import java.security.PrivateKey
import java.util.Base64

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun createUser(jwtToken: String, idToken: String): Resource<ResponseBody>
    suspend fun createPolicySetup(
        threshold: UInt,
        guardians: List<Guardian.SetupGuardian>,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicySetupApiResponse>

    suspend fun getPolicySetupHelper(
        threshold: UInt,
        prospectGuardians: List<Guardian.ProspectGuardian>
    ): PolicySetupHelper

    suspend fun createPolicy(
        setupHelper: PolicySetupHelper
    ): Resource<CreatePolicyApiResponse>


    suspend fun verifyToken(token: String): String?
    suspend fun saveJWT(jwtToken: String)
    suspend fun retrieveJWT(): String
    suspend fun checkJWTValid(jwtToken: String): Boolean

    suspend fun saveAccessToken(accessToken: String)
    suspend fun retrieveAccessToken(): String

    suspend fun confirmGuardianShip(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmGuardianshipApiResponse>

    suspend fun rejectVerification(
        participantId: ParticipantId
    ): Resource<RejectGuardianVerificationApiResponse>

    fun checkCodeMatches(
        verificationCode: String,
        transportKey: Base58EncodedGuardianPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean

    fun encryptShardWithGuardianKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ): ByteArray?

    suspend fun unlock(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<UnlockApiResponse>

    suspend fun lock(): Resource<LockApiResponse>

    suspend fun storeSecret(
        masterPublicKey: Base58EncodedMasterPublicKey,
        label: String,
        seedPhrase: String
    ): Resource<StoreSecretApiResponse>

    suspend fun deleteSecret(guid: VaultSecretId): Resource<DeleteSecretApiResponse>
    fun isUserEditingSecurityPlan(): Boolean
    fun setEditingSecurityPlan(editingPlan: Boolean)
    fun retrieveSecurityPlan(): SecurityPlanData?
    fun saveSecurityPlanData(securityPlanData: SecurityPlanData)
    fun clearSecurityPlanData()
    suspend fun initiateRecovery(secretIds: List<VaultSecretId>): Resource<InitiateRecoveryApiResponse>
    suspend fun cancelRecovery(): Resource<DeleteRecoveryApiResponse>
    suspend fun signUserOut()
    suspend fun submitRecoveryTotpVerification(
        participantId: ParticipantId,
        verificationCode: String
    ): Resource<SubmitRecoveryTotpVerificationApiResponse>

    suspend fun retrieveRecoveryShards(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<RetrieveRecoveryShardsApiResponse>

    fun recoverSecrets(
        encryptedSecrets: List<VaultSecret>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData
    ): List<RecoveredSeedPhrase>
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

    override suspend fun createPolicySetup(
        threshold: UInt,
        guardians: List<Guardian.SetupGuardian>,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicySetupApiResponse> {
        return retrieveApiResource {
            apiService.createOrUpdatePolicySetup(
                CreatePolicySetupApiRequest(
                    threshold,
                    guardians,
                    biometryVerificationId,
                    biometryData
                )
            )
        }
    }

    override suspend fun getPolicySetupHelper(
        threshold: UInt,
        prospectGuardians: List<Guardian.ProspectGuardian>
    ): PolicySetupHelper {
        val policySetupHelper = PolicySetupHelper.create(
            threshold = threshold,
            guardians = prospectGuardians,
        )

        projectLog(message = "Policy Setup Helper Created: $policySetupHelper")

        return policySetupHelper
    }

    override suspend fun createPolicy(
        setupHelper: PolicySetupHelper,
    ): Resource<CreatePolicyApiResponse> {
        val createPolicyApiRequest = CreatePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            guardianShards = setupHelper.guardianShards,
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun verifyToken(token: String): String? {
        val verifier = GoogleIdTokenVerifier.Builder(
            NetHttpTransport(), GsonFactory()
        )
            .setAudience(BuildConfig.GOOGLE_AUTH_CLIENT_IDS.toList())
            .build()

        val verifiedIdToken: GoogleIdToken? = verifier.verify(token)
        return verifiedIdToken?.payload?.subject
    }

    override suspend fun saveJWT(jwtToken: String) {
        storage.saveJWT(jwtToken)
    }

    override fun checkCodeMatches(
        verificationCode: String,
        transportKey: Base58EncodedGuardianPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ) =
        try {
            val dataToSign = verificationCode.generateVerificationCodeSignData(timeMillis)

            val externalDeviceKey =
                ExternalEncryptionKey.generateFromPublicKeyBase58(transportKey)

            externalDeviceKey.verify(
                signedData = dataToSign,
                signature = Base64.getDecoder().decode(signature.base64Encoded),
            )
        } catch (e: Exception) {
            false
        }

    override suspend fun confirmGuardianShip(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmGuardianshipApiResponse> {
        return retrieveApiResource {
            apiService.confirmGuardianship(
                participantId = participantId.value,
                confirmGuardianshipApiRequest = ConfirmGuardianshipApiRequest(
                    keyConfirmationSignature = Base64EncodedData(
                        Base64.getEncoder().encodeToString(keyConfirmationSignature)
                    ),
                    keyConfirmationTimeMillis = keyConfirmationTimeMillis
                )
            )
        }
    }

    override suspend fun rejectVerification(
        participantId: ParticipantId
    ): Resource<RejectGuardianVerificationApiResponse> {
        return retrieveApiResource {
            apiService.rejectVerification(participantId.value)
        }
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

    override suspend fun saveAccessToken(accessToken: String) {
        storage.saveAuthAccessToken(accessToken)
    }

    override suspend fun retrieveAccessToken(): String {
        return storage.retrieveAuthAccessToken()
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

    override suspend fun unlock(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<UnlockApiResponse> {
        return retrieveApiResource {
            apiService.unlock(
                UnlockApiRequest(biometryVerificationId, biometryData)
            )
        }
    }

    override suspend fun lock(): Resource<LockApiResponse> {
        return retrieveApiResource { apiService.lock() }
    }

    override suspend fun storeSecret(
        masterPublicKey: Base58EncodedMasterPublicKey,
        label: String,
        seedPhrase: String
    ): Resource<StoreSecretApiResponse> {

        val seedPhraseHash = seedPhrase.sha256()
        val encryptedSeedPhrase = ECIESManager.encryptMessage(
            dataToEncrypt = seedPhrase.toByteArray(),
            publicKeyBytes = Base58.base58Decode(masterPublicKey.value)
        )

        return retrieveApiResource {
            apiService.storeSecret(
                StoreSecretApiRequest(
                    label = label,
                    encryptedSeedPhrase = Base64EncodedData(
                        Base64.getEncoder().encodeToString(encryptedSeedPhrase)
                    ),
                    seedPhraseHash = seedPhraseHash
                )
            )
        }
    }

    override suspend fun deleteSecret(guid: VaultSecretId): Resource<DeleteSecretApiResponse> {
        return retrieveApiResource { apiService.deleteSecret(guid) }
    }

    override fun isUserEditingSecurityPlan() = storage.isEditingSecurityPlan()
    override fun setEditingSecurityPlan(editingPlan: Boolean) =
        storage.setEditingSecurityPlan(editingPlan)

    override fun retrieveSecurityPlan() = storage.retrieveSecurityPlan()

    override fun saveSecurityPlanData(securityPlanData: SecurityPlanData) =
        storage.setSecurityPlan(securityPlanData)

    override fun clearSecurityPlanData() = storage.clearSecurityPlanData()
    override suspend fun initiateRecovery(secretIds: List<VaultSecretId>): Resource<InitiateRecoveryApiResponse> {
        return retrieveApiResource { apiService.requestRecovery(InitiateRecoveryApiRequest(secretIds)) }
    }

    override suspend fun cancelRecovery(): Resource<DeleteRecoveryApiResponse> {
        return retrieveApiResource { apiService.deleteRecovery() }
    }

    override suspend fun signUserOut() {
        storage.clearJWT()
        storage.clearAuthAccessToken()
    }

    override suspend fun submitRecoveryTotpVerification(
        participantId: ParticipantId,
        verificationCode: String
    ): Resource<SubmitRecoveryTotpVerificationApiResponse> {
        val deviceKey = InternalDeviceKey(storage.retrieveDeviceKeyId())

        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()

        val dataToSign =
            verificationCode.toByteArray() + currentTimeInMillis.toString().toByteArray()
        val signature = deviceKey.sign(dataToSign).base64Encoded()

        return retrieveApiResource {
            apiService.submitRecoveryTotpVerification(
                participantId = participantId.value,
                apiRequest = SubmitRecoveryTotpVerificationApiRequest(
                    signature = signature,
                    timeMillis = currentTimeInMillis,
                    ownerDevicePublicKey = Base58EncodedDevicePublicKey(deviceKey.publicExternalRepresentation().value)
                )
            )
        }
    }

    override suspend fun retrieveRecoveryShards(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<RetrieveRecoveryShardsApiResponse> {

        return retrieveApiResource {
            apiService.retrieveRecoveryShards(
                apiRequest = RetrieveRecoveryShardsApiRequest(
                    biometryVerificationId = biometryVerificationId,
                    biometryData = biometryData
                )
            )
        }
    }

    override fun recoverSecrets(
        encryptedSecrets: List<VaultSecret>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData
    ): List<RecoveredSeedPhrase> {
        val deviceKey = InternalDeviceKey(storage.retrieveDeviceKeyId())

        val intermediateKeyShares = encryptedIntermediatePrivateKeyShards.map {
            Point(
                BigInteger(it.participantId.getBytes()),
                BigInteger(deviceKey.decrypt(it.encryptedShard.bytes))
            )
        }
        val intermediatePrivateKeyBigInt =
            SecretSharerUtils.recoverSecret(intermediateKeyShares, ORDER)

        val intermediatePrivateKey: PrivateKey =
            ECHelper.getPrivateKeyFromECBigIntAndCurve(intermediatePrivateKeyBigInt)

        val decryptedMasterPrivateKey: ByteArray = ECIESManager.decryptMessage(
            cipherData = encryptedMasterPrivateKey.bytes,
            privateKey = intermediatePrivateKey
        )

        val recreatedMasterEncryptionKey =
            EncryptionKey.generateFromPrivateKeyRaw(BigInteger(decryptedMasterPrivateKey))

        return encryptedSecrets.map {
            RecoveredSeedPhrase(
                guid = it.guid,
                label = it.label,
                seedPhrase = String(recreatedMasterEncryptionKey.decrypt(it.encryptedSeedPhrase.bytes)),
                createdAt = it.createdAt
            )
        }

    }
}