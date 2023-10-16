package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedGuardianPublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import ParticipantId
import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
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
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateRecoveryApiRequest
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.JwtToken
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.RejectGuardianVerificationApiResponse
import co.censo.shared.data.model.SecurityPlanData
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.StoreSecretApiRequest
import co.censo.shared.data.model.StoreSecretApiResponse
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.AuthUtil
import com.auth0.android.jwt.JWT
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun signInUser(jwtToken: String, idToken: String): Resource<ResponseBody>
    suspend fun createPolicySetup(
        threshold: UInt,
        guardians: List<Guardian.SetupGuardian>,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicySetupApiResponse>

    suspend fun createPolicy(
        threshold: UInt,
        guardians: List<Guardian.ProspectGuardian>
    ): Resource<CreatePolicyApiResponse>


    suspend fun verifyToken(token: String): String?
    suspend fun saveJWT(jwtToken: String)
    suspend fun retrieveJWT(): String
    suspend fun checkJWTValid(jwtToken: String): Boolean

    suspend fun confirmGuardianShip(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmGuardianshipApiResponse>

    suspend fun rejectVerification(
        participantId: ParticipantId
    ) : Resource<RejectGuardianVerificationApiResponse>

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
}

class OwnerRepositoryImpl(
    private val apiService: ApiService,
    private val storage: Storage,
    private val secureStorage: SecurePreferences,
    private val authUtil: AuthUtil
) : OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(): Resource<GetUserApiResponse> {
        return retrieveApiResource { apiService.user() }
    }

    override suspend fun signInUser(authId: String, idToken: String) =
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

    override suspend fun createPolicy(
        threshold: UInt,
        guardians: List<Guardian.ProspectGuardian>
    ): Resource<CreatePolicyApiResponse> {

        val setupHelper =  try {
            PolicySetupHelper.create(
                threshold = threshold,
                guardians = guardians
            )
        } catch (e: Exception) {
            return Resource.Error(exception = e)
        }

        val createPolicyApiRequest = CreatePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            guardianShards = setupHelper.guardianShards,
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun verifyToken(token: String): String? {
        return authUtil.verifyToken(token)
    }

    override suspend fun saveJWT(jwtToken: String) {
        secureStorage.saveJWT(jwtToken)
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

    override suspend fun retrieveJWT() = secureStorage.retrieveJWT()
    override suspend fun checkJWTValid(jwtToken: String): Boolean {
        return try {
            val jwtDecoded = JWT(jwtToken)
            authUtil.isJWTValid(jwtDecoded)
        } catch (e: Exception) {
            false
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
        secureStorage.clearJWT()
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
}