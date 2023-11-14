package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedGuardianPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import ParticipantId
import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.ORDER
import co.censo.shared.data.cryptography.Point
import co.censo.shared.data.cryptography.PolicySetupHelper
import co.censo.shared.data.cryptography.SecretSharerUtils
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.decryptFromByteArray
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
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateRecoveryApiRequest
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.JwtToken
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RecoveryIntent
import co.censo.shared.data.model.RejectGuardianVerificationApiResponse
import co.censo.shared.data.model.ReplacePolicyApiRequest
import co.censo.shared.data.model.ReplacePolicyApiResponse
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
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.auth0.android.jwt.JWT
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.math.BigInteger
import java.security.PrivateKey
import java.util.Base64

data class CreatePolicyParams(
    val guardianPublicKey: Base58EncodedGuardianPublicKey,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterPrivateKey: Base64EncodedData,
    val encryptedShard: Base64EncodedData,
    val participantId: ParticipantId
)

data class EncryptedSeedPhrase(
    val hash: String,
    val encrypted: Base64EncodedData
)

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetOwnerUserApiResponse>
    suspend fun signInUser(jwtToken: String, idToken: String): Resource<ResponseBody>

    suspend fun getCreatePolicyParams(
        ownerApprover: Guardian.ProspectGuardian
    ): Resource<CreatePolicyParams>

    suspend fun createPolicySetup(
        threshold: UInt,
        guardians: List<Guardian.SetupGuardian>,
    ): Resource<CreatePolicySetupApiResponse>

    suspend fun createPolicy(
        createPolicyParams: CreatePolicyParams,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicyApiResponse>

    suspend fun replacePolicy(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,

        threshold: UInt,
        guardians: List<Guardian.ProspectGuardian>
    ): Resource<ReplacePolicyApiResponse>

    suspend fun verifyToken(token: String): String?
    suspend fun saveJWT(jwtToken: String)
    fun retrieveJWT(): String
    fun checkJWTValid(jwtToken: String): Boolean

    fun verifyKeyConfirmationSignature(guardian: Guardian.ProspectGuardian): Boolean

    suspend fun confirmGuardianShip(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmGuardianshipApiResponse>

    suspend fun rejectVerification(
        participantId: ParticipantId
    ): Resource<RejectGuardianVerificationApiResponse>

    fun checkCodeMatches(
        encryptedTotpSecret: Base64EncodedData,
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

    suspend fun prolongUnlock(): Resource<ProlongUnlockApiResponse>

    suspend fun lock(): Resource<LockApiResponse>

    suspend fun encryptSecret(
        masterPublicKey: Base58EncodedMasterPublicKey,
        seedPhrase: String
    ): EncryptedSeedPhrase

    suspend fun storeSecret(
        label: String,
        seedPhrase: EncryptedSeedPhrase
    ): Resource<StoreSecretApiResponse>

    suspend fun deleteSecret(guid: VaultSecretId): Resource<DeleteSecretApiResponse>
    suspend fun deleteUser(participantId: ParticipantId?, deletingApproverUser: Boolean = false): Resource<Unit>

    fun isUserEditingSecurityPlan(): Boolean
    fun setEditingSecurityPlan(editingPlan: Boolean)
    fun retrieveSecurityPlan(): SecurityPlanData?
    fun saveSecurityPlanData(securityPlanData: SecurityPlanData)
    fun clearSecurityPlanData()
    suspend fun initiateRecovery(intent: RecoveryIntent): Resource<InitiateRecoveryApiResponse>
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

    suspend fun recoverSecrets(
        encryptedSecrets: List<VaultSecret>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData
    ): List<RecoveredSeedPhrase>
}

class OwnerRepositoryImpl(
    private val apiService: ApiService,
    private val secureStorage: SecurePreferences,
    private val authUtil: AuthUtil,
    private val keyRepository: KeyRepository
) : OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(): Resource<GetOwnerUserApiResponse> {
        return retrieveApiResource { apiService.ownerUser() }
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
    ): Resource<CreatePolicySetupApiResponse> {
        return retrieveApiResource {
            apiService.createOrUpdatePolicySetup(
                CreatePolicySetupApiRequest(
                    threshold,
                    guardians
                )
            )
        }
    }

    override suspend fun getCreatePolicyParams(
        ownerApprover: Guardian.ProspectGuardian
    ): Resource<CreatePolicyParams> {
        return try {
            PolicySetupHelper.create(
                masterEncryptionKey = EncryptionKey.generateRandomKey(),
                threshold = 1U,
                guardians = listOf(ownerApprover)
            ). let {
                Resource.Success(
                    CreatePolicyParams(
                        guardianPublicKey = (ownerApprover.status as GuardianStatus.ImplicitlyOwner).guardianPublicKey,
                        intermediatePublicKey = it.intermediatePublicKey,
                        masterEncryptionPublicKey = it.masterEncryptionPublicKey,
                        encryptedMasterPrivateKey = it.encryptedMasterKey,
                        encryptedShard = it.guardianShards.first().encryptedShard,
                        participantId = it.guardianShards.first().participantId
                    )
                )
            }

        } catch (e: Exception) {
            e.sendError("CreatePolicyParams")
            Resource.Error(exception = e)
        }
    }

    override suspend fun createPolicy(
        createPolicyParams: CreatePolicyParams,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicyApiResponse> {


        val createPolicyApiRequest = CreatePolicyApiRequest(
            masterEncryptionPublicKey = createPolicyParams.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = createPolicyParams.encryptedMasterPrivateKey,
            intermediatePublicKey = createPolicyParams.intermediatePublicKey,
            participantId = createPolicyParams.participantId,
            encryptedShard = createPolicyParams.encryptedShard,
            guardianPublicKey = createPolicyParams.guardianPublicKey,
            biometryVerificationId = biometryVerificationId,
            biometryData = biometryData,
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun replacePolicy(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        threshold: UInt,
        guardians: List<Guardian.ProspectGuardian>,
    ): Resource<ReplacePolicyApiResponse> {
        val intermediateEncryptionKey = recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards)
        val masterEncryptionKey = recoverMasterEncryptionKey(encryptedMasterPrivateKey, intermediateEncryptionKey)

        val setupHelper = try {
            PolicySetupHelper.create(
                threshold = threshold,
                guardians = guardians,
                masterEncryptionKey = masterEncryptionKey,
                previousIntermediateKey = intermediateEncryptionKey
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicy)
            return Resource.Error(exception = e)
        }

        val replacePolicyApiRequest = ReplacePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            guardianShards = setupHelper.guardianShards,
            signatureByPreviousIntermediateKey = setupHelper.signatureByPreviousIntermediateKey!!
        )

        return retrieveApiResource { apiService.replacePolicy(replacePolicyApiRequest) }
    }

    override suspend fun verifyToken(token: String): String? {
        return authUtil.verifyToken(token)
    }

    override suspend fun saveJWT(jwtToken: String) {
        secureStorage.saveJWT(jwtToken)
    }

    override fun checkCodeMatches(
        encryptedTotpSecret: Base64EncodedData,
        transportKey: Base58EncodedGuardianPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean =
        try {
            val externalDeviceKey =
                ExternalEncryptionKey.generateFromPublicKeyBase58(transportKey)

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

    override fun retrieveJWT() = secureStorage.retrieveJWT()
    override fun checkJWTValid(jwtToken: String): Boolean {
        return try {
            val jwtDecoded = JWT(jwtToken)
            authUtil.isJWTValid(jwtDecoded)
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.JWTToken)
            false
        }
    }

    override fun verifyKeyConfirmationSignature(guardian: Guardian.ProspectGuardian): Boolean {
        return try {
            when (val status = guardian.status) {
                is GuardianStatus.Confirmed -> {
                    val deviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())
                    deviceKey.verify(
                        status.guardianPublicKey.getBytes() + guardian.participantId.getBytes() + status.timeMillis.toString().toByteArray(),
                        status.guardianKeySignature.bytes
                    )
                }
                is GuardianStatus.ImplicitlyOwner -> true
                else -> false
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.EncryptShard)
            false
        }
    }

    override fun encryptShardWithGuardianKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ): ByteArray? {
        return try {
            val guardianDevicePublicKey = transportKey.ecPublicKey

            val decryptedShard = InternalDeviceKey(secureStorage.retrieveDeviceKeyId()).decrypt(
                Base64.getDecoder().decode(deviceEncryptedShard.base64Encoded)
            )

            ECIESManager.encryptMessage(
                dataToEncrypt = decryptedShard,
                publicKeyBytes = ECPublicKeyDecoder.extractUncompressedPublicKey(
                    guardianDevicePublicKey.encoded
                )
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.EncryptShard)
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

    override suspend fun prolongUnlock(): Resource<ProlongUnlockApiResponse> {
        return retrieveApiResource { apiService.prolongUnlock() }
    }

    override suspend fun lock(): Resource<LockApiResponse> {
        return retrieveApiResource { apiService.lock() }
    }

    override suspend fun encryptSecret(
        masterPublicKey: Base58EncodedMasterPublicKey,
        seedPhrase: String
    ): EncryptedSeedPhrase {
        val encodedData = BIP39.phraseToBinaryData(seedPhrase)

        val encryptedSeedPhrase = ECIESManager.encryptMessage(
            dataToEncrypt = encodedData,
            publicKeyBytes = Base58.base58Decode(masterPublicKey.value)
        )

        return EncryptedSeedPhrase(
            hash = encodedData.sha256(),
            encrypted = encryptedSeedPhrase.base64Encoded()
        )
    }

    override suspend fun storeSecret(
        label: String,
        seedPhrase: EncryptedSeedPhrase
    ): Resource<StoreSecretApiResponse> {

        return retrieveApiResource {
            apiService.storeSecret(
                StoreSecretApiRequest(
                    label = label,
                    encryptedSeedPhrase = seedPhrase.encrypted,
                    seedPhraseHash = seedPhrase.hash,
                )
            )
        }
    }

    override suspend fun deleteSecret(guid: VaultSecretId): Resource<DeleteSecretApiResponse> {
        return retrieveApiResource { apiService.deleteSecret(guid) }
    }

    override suspend fun deleteUser(participantId: ParticipantId?, deletingApproverUser: Boolean): Resource<Unit> {
        val response = retrieveApiResource { apiService.deleteUser() }

        if (response is Resource.Success) {
            try {
                keyRepository.deleteDeviceKeyIfPresent(secureStorage.retrieveDeviceKeyId())
                if (participantId != null && !deletingApproverUser) {
                    keyRepository.deleteSavedKeyFromCloud(participantId)
                }
                secureStorage.clearDeviceKeyId()
                signUserOut()
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.DeleteUser)
            }
        }

        return response
    }

    override fun isUserEditingSecurityPlan() = secureStorage.isEditingSecurityPlan()
    override fun setEditingSecurityPlan(editingPlan: Boolean) =
        secureStorage.setEditingSecurityPlan(editingPlan)

    override fun retrieveSecurityPlan() = secureStorage.retrieveSecurityPlan()

    override fun saveSecurityPlanData(securityPlanData: SecurityPlanData) =
        secureStorage.setSecurityPlan(securityPlanData)

    override fun clearSecurityPlanData() = secureStorage.clearSecurityPlanData()
    override suspend fun initiateRecovery(intent: RecoveryIntent): Resource<InitiateRecoveryApiResponse> {
        return retrieveApiResource { apiService.requestRecovery(InitiateRecoveryApiRequest(intent)) }
    }

    override suspend fun cancelRecovery(): Resource<DeleteRecoveryApiResponse> {
        return retrieveApiResource { apiService.deleteRecovery() }
    }

    override suspend fun signUserOut() {
        authUtil.signOut()
        secureStorage.clearJWT()
    }

    override suspend fun submitRecoveryTotpVerification(
        participantId: ParticipantId,
        verificationCode: String
    ): Resource<SubmitRecoveryTotpVerificationApiResponse> {
        val deviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())

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

    override suspend fun recoverSecrets(
        encryptedSecrets: List<VaultSecret>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
    ): List<RecoveredSeedPhrase> {
        val intermediateEncryptionKey = recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards)
        val masterEncryptionKey = recoverMasterEncryptionKey(encryptedMasterPrivateKey, intermediateEncryptionKey)

        return encryptedSecrets.map {
            RecoveredSeedPhrase(
                guid = it.guid,
                label = it.label,
                seedPhrase = BIP39.binaryDataToWords(
                    masterEncryptionKey.decrypt(it.encryptedSeedPhrase.bytes)
                ).joinToString(" "),
                createdAt = it.createdAt
            )
        }
    }

    private suspend fun recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>): PrivateKey {
        val ownerDeviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())

        val intermediateKeyShares = encryptedIntermediatePrivateKeyShards.map {
            val encryptionKey = when (it.isOwnerShard) {
                true -> {
                    val ownerApproverKeyResource = keyRepository.retrieveKeyFromCloud(it.participantId)
                    if (ownerApproverKeyResource is Resource.Error) {
                        throw ownerApproverKeyResource.exception!!
                    } else {
                        val encryptedKey = ownerApproverKeyResource.data!!
                        val base58EncodedPrivateKey =
                            encryptedKey.decryptFromByteArray(keyRepository.retrieveSavedDeviceId())

                        EncryptionKey.generateFromPrivateKeyRaw(base58EncodedPrivateKey.bigInt())
                    }
                }
                false -> ownerDeviceKey
            }
            Point(
                it.participantId.bigInt(),
                BigInteger(encryptionKey.decrypt(it.encryptedShard.bytes))
            )
        }

        return ECHelper.getPrivateKeyFromECBigIntAndCurve(
            SecretSharerUtils.recoverSecret(intermediateKeyShares, ORDER)
        )
    }

    private fun recoverMasterEncryptionKey(
        encryptedMasterKey: Base64EncodedData,
        intermediateKey: PrivateKey
    ) = EncryptionKey.generateFromPrivateKeyRaw(
        BigInteger(
            ECIESManager.decryptMessage(
                cipherData = encryptedMasterKey.bytes,
                privateKey = intermediateKey
            )
        )
    )
}