package co.censo.shared.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedApproverPublicKey
import Base58EncodedBeneficiaryPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import ParticipantId
import SeedPhraseId
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
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.sha256
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.CompleteOwnerApprovershipApiRequest
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.ConfirmApprovershipApiRequest
import co.censo.shared.data.model.ConfirmApprovershipApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiRequest
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.DeleteSeedPhraseApiResponse
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateAccessApiRequest
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.ActivateBeneficiaryApiRequest
import co.censo.shared.data.model.ActivateBeneficiaryApiResponse
import co.censo.shared.data.model.ApproverPublicKey
import co.censo.shared.data.model.Authentication
import co.censo.shared.data.model.BeneficiaryEncryptedKey
import co.censo.shared.data.model.CancelAuthenticationResetApiResponse
import co.censo.shared.data.model.DeletePolicySetupApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.GetImportEncryptedDataApiResponse
import co.censo.shared.data.model.GetSeedPhraseApiResponse
import co.censo.shared.data.model.InitiateAuthenticationResetApiResponse
import co.censo.shared.data.model.InviteBeneficiaryApiRequest
import co.censo.shared.data.model.InviteBeneficiaryApiResponse
import co.censo.shared.data.model.OwnerProof
import co.censo.shared.data.model.RejectApproverVerificationApiResponse
import co.censo.shared.data.model.RejectBeneficiaryVerificationApiResponse
import co.censo.shared.data.model.ReplaceAuthenticationApiRequest
import co.censo.shared.data.model.ReplaceAuthenticationApiResponse
import co.censo.shared.data.model.ReplacePolicyApiRequest
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.ReplacePolicyShardsApiRequest
import co.censo.shared.data.model.ReplacePolicyShardsApiResponse
import co.censo.shared.data.model.ResetLoginIdApiRequest
import co.censo.shared.data.model.ResetLoginIdApiResponse
import co.censo.shared.data.model.ResetToken
import co.censo.shared.data.model.RetrieveAccessShardsApiRequest
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.model.RetrieveAuthTypeApiRequest
import co.censo.shared.data.model.RetrieveAuthTypeApiResponse
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.StoreSeedPhraseApiRequest
import co.censo.shared.data.model.StoreSeedPhraseApiResponse
import co.censo.shared.data.model.SubmitPurchaseApiRequest
import co.censo.shared.data.model.SubmitPurchaseApiResponse
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.model.SeedPhraseData
import co.censo.shared.data.model.SetPromoCodeApiRequest
import co.censo.shared.data.model.TimelockApiResponse
import co.censo.shared.data.model.UpdateSeedPhraseApiRequest
import co.censo.shared.data.model.UpdateSeedPhraseApiResponse
import co.censo.shared.data.model.toByteArray
import co.censo.shared.data.model.toSeedPhraseData
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.auth0.android.jwt.JWT
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import java.math.BigInteger
import java.security.KeyPair
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.util.Base64

data class CreatePolicyParams(
    val approverPublicKey: Base58EncodedApproverPublicKey,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val approverPublicKeySignatureByIntermediateKey: Base64EncodedData,
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterPrivateKey: Base64EncodedData,
    val encryptedShard: Base64EncodedData,
    val participantId: ParticipantId,
    val masterKeySignature: Base64EncodedData,
)

data class EncryptedSeedPhrase(
    val hash: String,
    val encrypted: Base64EncodedData
)

enum class AuthState {
    LOGGED_IN,
    LOGGED_OUT
}

interface OwnerRepository {

    //region OwnerState flow
    fun updateAuthState(authState: AuthState)
    fun getOwnerStateValue(): OwnerState
    fun updateOwnerState(ownerState: OwnerState)
    suspend fun collectOwnerState(collector: FlowCollector<OwnerState>)
    //endregion

    suspend fun health(): Resource<Unit>
    suspend fun retrieveUser(): Resource<GetOwnerUserApiResponse>
    suspend fun signInUser(idToken: String): Resource<Unit>
    suspend fun createDevice(): Resource<Unit>
    suspend fun retrieveAuthType(resetTokens: List<ResetToken>): Resource<RetrieveAuthTypeApiResponse>
    suspend fun resetLoginId(
        idToken: String,
        resetTokens: List<ResetToken>,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
        password: Authentication.Password?,
    ): Resource<ResetLoginIdApiResponse>

    suspend fun getCreatePolicyParams(
        ownerApprover: Approver.ProspectApprover,
        ownerApproverKey: Base58EncodedApproverPublicKey,
        ownerApproverEncryptedPrivateKey: ByteArray,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<CreatePolicyParams>

    suspend fun createPolicySetup(
        threshold: UInt,
        approvers: List<Approver.SetupApprover>,
    ): Resource<CreatePolicySetupApiResponse>

    suspend fun deletePolicySetup(): Resource<DeletePolicySetupApiResponse>

    suspend fun createPolicy(
        createPolicyParams: CreatePolicyParams,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
    ): Resource<CreatePolicyApiResponse>

    suspend fun replacePolicy(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        threshold: UInt,
        approvers: List<Approver.ProspectApprover>,
        ownerApproverEncryptedPrivateKey: ByteArray,
        ownerApproverKey: Base58EncodedApproverPublicKey,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<ReplacePolicyApiResponse>

    suspend fun verifyApproverPublicKeysSignature(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        approverPublicKeys: List<Base58EncodedApproverPublicKey>,
        approverPublicKeysSignature: Base64EncodedData
    ): Boolean

    suspend fun replaceShards(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        threshold: UInt,
        approverPublicKeys: Map<ParticipantId, Base58EncodedApproverPublicKey>,
        ownerApproverEncryptedPrivateKey: ByteArray,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<ReplacePolicyShardsApiResponse>

    suspend fun verifyToken(token: String): String?
    suspend fun saveJWT(jwtToken: String)
    fun retrieveJWT(): String
    fun clearJWT()
    fun checkJWTValid(jwtToken: String): Boolean

    fun verifyKeyConfirmationSignature(approver: Approver.ProspectApprover): Boolean

    suspend fun confirmApprovership(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmApprovershipApiResponse>

    suspend fun rejectVerification(
        participantId: ParticipantId
    ): Resource<RejectApproverVerificationApiResponse>

    fun checkCodeMatches(
        encryptedTotpSecret: Base64EncodedData,
        transportKey: Base58EncodedPublicKey,
        timeMillis: Long,
        signature: Base64EncodedData
    ): Boolean

    fun encryptShardWithApproverKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ): ByteArray?

    suspend fun unlock(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<UnlockApiResponse>

    suspend fun prolongUnlock(): Resource<ProlongUnlockApiResponse>

    suspend fun lock(): Resource<LockApiResponse>

    suspend fun encryptSeedPhrase(
        masterPublicKey: Base58EncodedMasterPublicKey,
        seedPhraseData: SeedPhraseData
    ): EncryptedSeedPhrase

    suspend fun storeSeedPhrase(
        label: String,
        seedPhrase: EncryptedSeedPhrase
    ): Resource<StoreSeedPhraseApiResponse>

    suspend fun deleteSeedPhrase(guid: SeedPhraseId): Resource<DeleteSeedPhraseApiResponse>

    suspend fun updateSeedPhrase(
        guid: SeedPhraseId,
        label: String
    ): Resource<UpdateSeedPhraseApiResponse>

    suspend fun deleteUser(participantId: ParticipantId?): Resource<Unit>

    suspend fun initiateAccess(intent: AccessIntent): Resource<InitiateAccessApiResponse>
    suspend fun cancelAccess(): Resource<DeleteAccessApiResponse>
    suspend fun signUserOut()
    suspend fun submitAccessTotpVerification(
        participantId: ParticipantId,
        verificationCode: String
    ): Resource<SubmitAccessTotpVerificationApiResponse>

    suspend fun retrieveAccessShards(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<RetrieveAccessShardsApiResponse>

    suspend fun recoverSeedPhrases(
        encryptedSeedPhrases: List<SeedPhrase>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        language: BIP39.WordListLanguage?
    ): List<RecoveredSeedPhrase>

    suspend fun submitPurchase(
        purchaseToken: String
    ): Resource<SubmitPurchaseApiResponse>

    suspend fun completeApproverOwnership(
        participantId: ParticipantId,
        completeOwnerApprovershipApiRequest: CompleteOwnerApprovershipApiRequest
    ): Resource<CompleteOwnerApprovershipApiResponse>

    suspend fun acceptImport(channel: String, ownerProof: OwnerProof): Resource<Unit>

    suspend fun checkForCompletedImport(channel: String): Resource<GetImportEncryptedDataApiResponse>

    suspend fun enableTimelock(): Resource<TimelockApiResponse>

    suspend fun disableTimelock(): Resource<TimelockApiResponse>

    suspend fun cancelDisableTimelock(): Resource<Unit>

    suspend fun requestAuthenticationReset(): Resource<InitiateAuthenticationResetApiResponse>
    suspend fun cancelAuthenticationReset(): Resource<CancelAuthenticationResetApiResponse>
    suspend fun replaceAuthentication(authentication: Authentication): Resource<ReplaceAuthenticationApiResponse>
    suspend fun setPromoCode(code: String) : Resource<Unit>
    suspend fun inviteBeneficiary(
        label: String,
        deviceEncryptedTotpSecret: Base64EncodedData
    ): Resource<InviteBeneficiaryApiResponse>

    suspend fun activateBeneficiary(
        ownerParticipantId: ParticipantId,
        entropy: Base64EncodedData,
        beneficiaryPublicKey: Base58EncodedBeneficiaryPublicKey,
        approverPublicKeys: List<ApproverPublicKey>
    ): Resource<ActivateBeneficiaryApiResponse>

    suspend fun rejectBeneficiaryVerification(): Resource<RejectBeneficiaryVerificationApiResponse>
}

class OwnerRepositoryImpl(
    private val apiService: ApiService,
    private val secureStorage: SecurePreferences,
    private val authUtil: AuthUtil,
    private val keyRepository: KeyRepository,
    private val totpGenerator: TotpGenerator
) : OwnerRepository, BaseRepository() {

    //region OwnerState flow
    private val ownerStateFlow = MutableStateFlow<OwnerState>(OwnerState.Empty)
    private val authStateFlow = MutableStateFlow(AuthState.LOGGED_OUT)
    override fun updateAuthState(authState: AuthState) {
        authStateFlow.value = authState
    }

    override fun getOwnerStateValue(): OwnerState = ownerStateFlow.value
    override suspend fun collectOwnerState(collector: FlowCollector<OwnerState>) {
        ownerStateFlow.collect(collector)
    }

    override fun updateOwnerState(ownerState: OwnerState) {
        if (authStateFlow.value == AuthState.LOGGED_IN) {
            ownerStateFlow.value = ownerState
        }
    }
    //endregion

    override suspend fun health(): Resource<Unit> {
        return retrieveApiResource { apiService.health() }
    }

    override suspend fun retrieveUser(): Resource<GetOwnerUserApiResponse> {
        return retrieveApiResource { apiService.ownerUser() }
    }

    override suspend fun signInUser(idToken: String) =
        retrieveApiResource {
            apiService.signIn(
                SignInApiRequest(
                    identityToken = IdentityToken(idToken.sha256()),
                )
            )
        }.map {
            updateAuthState(AuthState.LOGGED_IN)
        }

    override suspend fun createDevice(): Resource<Unit> {
        return retrieveApiResource {
            apiService.createDevice()
        }
    }

    override suspend fun retrieveAuthType(resetTokens: List<ResetToken>): Resource<RetrieveAuthTypeApiResponse> {
        return retrieveApiResource {
            apiService.retrieveAuthType(
                RetrieveAuthTypeApiRequest(
                    resetTokens = resetTokens
                )
            )
        }
    }

    override suspend fun resetLoginId(
        idToken: String,
        resetTokens: List<ResetToken>,
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry,
        password: Authentication.Password?
    ): Resource<ResetLoginIdApiResponse> {
        return retrieveApiResource {
            apiService.resetLoginId(
                ResetLoginIdApiRequest(
                    identityToken = IdentityToken(idToken.sha256()),
                    resetTokens = resetTokens,
                    biometryVerificationId = biometryVerificationId,
                    biometryData = biometryData,
                    password = password,
                )
            )
        }
    }

    override suspend fun createPolicySetup(
        threshold: UInt,
        approvers: List<Approver.SetupApprover>,
    ): Resource<CreatePolicySetupApiResponse> {
        return retrieveApiResource {
            apiService.createOrUpdatePolicySetup(
                CreatePolicySetupApiRequest(
                    threshold,
                    approvers
                )
            )
        }
    }

    override suspend fun deletePolicySetup(): Resource<DeletePolicySetupApiResponse> {
        return retrieveApiResource {
            apiService.deletePolicySetup()
        }
    }

    override suspend fun getCreatePolicyParams(
        ownerApprover: Approver.ProspectApprover,
        ownerApproverKey: Base58EncodedApproverPublicKey,
        ownerApproverEncryptedPrivateKey: ByteArray,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<CreatePolicyParams> {
        return try {
            PolicySetupHelper.create(
                masterEncryptionKey = EncryptionKey.generateRandomKey(),
                threshold = 1U,
                approvers = listOf(ownerApprover),
                ownerApproverEncryptedPrivateKey = ownerApproverEncryptedPrivateKey,
                ownerApproverKey = ownerApproverKey,
                entropy = entropy,
                deviceKeyId = deviceKeyId
            ).let {

                Resource.Success(
                    CreatePolicyParams(
                        approverPublicKey = ownerApproverKey,
                        intermediatePublicKey = it.intermediatePublicKey,
                        approverPublicKeySignatureByIntermediateKey = it.approverKeysSignatureByIntermediateKey,
                        masterEncryptionPublicKey = it.masterEncryptionPublicKey,
                        encryptedMasterPrivateKey = it.encryptedMasterKey,
                        encryptedShard = it.approverShards.first().encryptedShard,
                        participantId = it.approverShards.first().participantId,
                        masterKeySignature = it.masterKeySignature
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
            approverPublicKey = createPolicyParams.approverPublicKey,
            approverPublicKeySignatureByIntermediateKey = createPolicyParams.approverPublicKeySignatureByIntermediateKey,
            biometryVerificationId = biometryVerificationId,
            biometryData = biometryData,
            masterKeySignature = createPolicyParams.masterKeySignature
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun replacePolicy(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        threshold: UInt,
        approvers: List<Approver.ProspectApprover>,
        ownerApproverEncryptedPrivateKey: ByteArray,
        ownerApproverKey: Base58EncodedApproverPublicKey,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<ReplacePolicyApiResponse> {
        val intermediateEncryptionKey =
            recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards)
        val masterEncryptionKey =
            recoverMasterEncryptionKey(encryptedMasterPrivateKey, intermediateEncryptionKey)

        val setupHelper = try {
            PolicySetupHelper.create(
                threshold = threshold,
                approvers = approvers,
                masterEncryptionKey = masterEncryptionKey,
                previousIntermediateKey = intermediateEncryptionKey,
                ownerApproverEncryptedPrivateKey = ownerApproverEncryptedPrivateKey,
                ownerApproverKey = ownerApproverKey,
                entropy = entropy,
                deviceKeyId = deviceKeyId
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicy)
            return Resource.Error(exception = e)
        }

        val replacePolicyApiRequest = ReplacePolicyApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            approverShards = setupHelper.approverShards,
            approverPublicKeysSignatureByIntermediateKey = setupHelper.approverKeysSignatureByIntermediateKey,
            signatureByPreviousIntermediateKey = setupHelper.signatureByPreviousIntermediateKey!!,
            masterKeySignature = setupHelper.masterKeySignature
        )

        return retrieveApiResource { apiService.replacePolicy(replacePolicyApiRequest) }
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun verifyApproverPublicKeysSignature(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        approverPublicKeys: List<Base58EncodedApproverPublicKey>,
        approverPublicKeysSignature: Base64EncodedData
    ): Boolean {
        val intermediatePrivateKey =
            recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards)

        return runCatching {
            val encryptionKey = intermediatePrivateKey.let {
                val privateKey = intermediatePrivateKey as ECPrivateKey
                val publicKey = ECPublicKeyDecoder.getPublicKeyFromPrivateKey(privateKey)
                EncryptionKey(KeyPair(publicKey, privateKey))
            }

            encryptionKey.verify(
                signedData = approverPublicKeys
                    .sortedBy { it.value }
                    .map { it.getBytes() }
                    .reduce { acc, key -> acc + key },
                signature = approverPublicKeysSignature.bytes
            )
        }.getOrNull() ?: false
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun replaceShards(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        threshold: UInt,
        approverPublicKeys: Map<ParticipantId, Base58EncodedApproverPublicKey>,
        ownerApproverEncryptedPrivateKey: ByteArray,
        entropy: Base64EncodedData,
        deviceKeyId: String
    ): Resource<ReplacePolicyShardsApiResponse> {
        val intermediateEncryptionKey =
            recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards)
        val masterEncryptionKey =
            recoverMasterEncryptionKey(encryptedMasterPrivateKey, intermediateEncryptionKey)

        val setupHelper = try {
            PolicySetupHelper.create(
                threshold = threshold,
                approverPublicKeys = approverPublicKeys,
                masterEncryptionKey = masterEncryptionKey,
                previousIntermediateKey = intermediateEncryptionKey,
                ownerApproverEncryptedPrivateKey = ownerApproverEncryptedPrivateKey,
                entropy = entropy,
                deviceKeyId = deviceKeyId,
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicy)
            return Resource.Error(exception = e)
        }

        val replacePolicyApiRequest = ReplacePolicyShardsApiRequest(
            masterEncryptionPublicKey = setupHelper.masterEncryptionPublicKey,
            encryptedMasterPrivateKey = setupHelper.encryptedMasterKey,
            intermediatePublicKey = setupHelper.intermediatePublicKey,
            approverShards = setupHelper.approverShards.map {
                ReplacePolicyShardsApiRequest.ApproverShard(
                    it.participantId, it.encryptedShard, approverPublicKeys[it.participantId]!!
                )
            },
            approverPublicKeysSignatureByIntermediateKey = setupHelper.approverKeysSignatureByIntermediateKey,
            signatureByPreviousIntermediateKey = setupHelper.signatureByPreviousIntermediateKey!!,
            masterKeySignature = setupHelper.masterKeySignature
        )

        return retrieveApiResource { apiService.replacePolicyShards(replacePolicyApiRequest) }
    }

    override suspend fun verifyToken(token: String): String? {
        return authUtil.verifyToken(token)
    }

    override suspend fun saveJWT(jwtToken: String) {
        secureStorage.saveJWT(jwtToken)
    }

    override fun checkCodeMatches(
        encryptedTotpSecret: Base64EncodedData,
        transportKey: Base58EncodedPublicKey,
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
                val code = totpGenerator.generateCode(
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

    override suspend fun confirmApprovership(
        participantId: ParticipantId,
        keyConfirmationSignature: ByteArray,
        keyConfirmationTimeMillis: Long
    ): Resource<ConfirmApprovershipApiResponse> {
        return retrieveApiResource {
            apiService.confirmApprovership(
                participantId = participantId.value,
                confirmApprovershipApiRequest = ConfirmApprovershipApiRequest(
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
    ): Resource<RejectApproverVerificationApiResponse> {
        return retrieveApiResource {
            apiService.rejectVerification(participantId.value)
        }
    }

    override fun retrieveJWT() = secureStorage.retrieveJWT()
    override fun clearJWT() = secureStorage.clearJWT()
    override fun checkJWTValid(jwtToken: String): Boolean {
        return try {
            val jwtDecoded = JWT(jwtToken)
            authUtil.isJWTValid(jwtDecoded)
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.JWTToken)
            false
        }
    }

    override fun verifyKeyConfirmationSignature(approver: Approver.ProspectApprover): Boolean {
        return try {
            when (val status = approver.status) {
                is ApproverStatus.Confirmed -> {
                    val deviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())
                    deviceKey.verify(
                        status.approverPublicKey.getBytes() + approver.participantId.getBytes() + status.timeMillis.toString()
                            .toByteArray(),
                        status.approverKeySignature.bytes
                    )
                }

                is ApproverStatus.OwnerAsApprover -> true
                else -> false
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.VerifyKeyConfirmation)
            false
        }
    }

    override fun encryptShardWithApproverKey(
        deviceEncryptedShard: Base64EncodedData,
        transportKey: Base58EncodedDevicePublicKey
    ): ByteArray? {
        val decryptedShard = try {
            InternalDeviceKey(secureStorage.retrieveDeviceKeyId()).decrypt(
                Base64.getDecoder().decode(deviceEncryptedShard.base64Encoded)
            )
        } catch (t: Throwable) {
            Exception("Unable to decrypt shard").sendError(CrashReportingUtil.DecryptShard)
            return null
        }

        return try {
            val approverDevicePublicKey = transportKey.ecPublicKey

            ECIESManager.encryptMessage(
                dataToEncrypt = decryptedShard,
                publicKeyBytes = ECPublicKeyDecoder.extractUncompressedPublicKey(
                    approverDevicePublicKey.encoded
                )
            )
        } catch (t: Throwable) {
            Exception("Unable to re-encrypt shard").sendError(CrashReportingUtil.EncryptShard)
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

    override suspend fun enableTimelock(): Resource<TimelockApiResponse> {
        return retrieveApiResource { apiService.enableTimelock() }
    }

    override suspend fun disableTimelock(): Resource<TimelockApiResponse> {
        return retrieveApiResource { apiService.disableTimelock() }
    }

    override suspend fun cancelDisableTimelock(): Resource<Unit> {
        return retrieveApiResource { apiService.cancelDisableTimelock() }
    }

    override suspend fun requestAuthenticationReset(): Resource<InitiateAuthenticationResetApiResponse> {
        return retrieveApiResource { apiService.requestAuthenticationReset() }
    }

    override suspend fun cancelAuthenticationReset(): Resource<CancelAuthenticationResetApiResponse> {
        return retrieveApiResource { apiService.cancelAuthenticationReset() }
    }

    override suspend fun replaceAuthentication(authentication: Authentication): Resource<ReplaceAuthenticationApiResponse> {
        return retrieveApiResource {
            apiService.replaceAuthentication(
                ReplaceAuthenticationApiRequest(authentication)
            )
        }
    }

    override suspend fun encryptSeedPhrase(
        masterPublicKey: Base58EncodedMasterPublicKey,
        seedPhraseData: SeedPhraseData
    ): EncryptedSeedPhrase {
        val encodedData = seedPhraseData.toByteArray()
        val encryptedSeedPhrase = ECIESManager.encryptMessage(
            dataToEncrypt = encodedData,
            publicKeyBytes = Base58.base58Decode(masterPublicKey.value)
        )

        return EncryptedSeedPhrase(
            hash = encodedData.sha256(),
            encrypted = encryptedSeedPhrase.base64Encoded()
        )
    }

    override suspend fun storeSeedPhrase(
        label: String,
        seedPhrase: EncryptedSeedPhrase
    ): Resource<StoreSeedPhraseApiResponse> {

        return retrieveApiResource {
            apiService.storeSeedPhrase(
                StoreSeedPhraseApiRequest(
                    label = label,
                    encryptedSeedPhrase = seedPhrase.encrypted,
                    seedPhraseHash = seedPhrase.hash,
                )
            )
        }
    }

    override suspend fun deleteSeedPhrase(guid: SeedPhraseId): Resource<DeleteSeedPhraseApiResponse> {
        return retrieveApiResource { apiService.deleteSeedPhrase(guid) }
    }

    override suspend fun updateSeedPhrase(
        guid: SeedPhraseId,
        label: String
    ): Resource<UpdateSeedPhraseApiResponse> {
        return retrieveApiResource {
            apiService.updateSeedPhrase(
                guid,
                UpdateSeedPhraseApiRequest(label)
            )
        }
    }

    override suspend fun deleteUser(participantId: ParticipantId?): Resource<Unit> {
        val response = retrieveApiResource { apiService.deleteUser() }

        if (response is Resource.Success) {
            try {
                keyRepository.deleteDeviceKeyIfPresent(secureStorage.retrieveDeviceKeyId())
                if (participantId != null) {
                    keyRepository.deleteSavedKeyFromCloud(participantId.value)
                }
                secureStorage.clearDeviceKeyId()
                signUserOut()
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.DeleteUser)
            }
        }

        return response
    }

    override suspend fun initiateAccess(intent: AccessIntent): Resource<InitiateAccessApiResponse> {
        return retrieveApiResource { apiService.requestAccess(InitiateAccessApiRequest(intent)) }
    }

    override suspend fun cancelAccess(): Resource<DeleteAccessApiResponse> {
        return retrieveApiResource { apiService.deleteAccess() }
    }

    override suspend fun signUserOut() {
        updateOwnerState(OwnerState.Empty)
        updateAuthState(AuthState.LOGGED_OUT)
        secureStorage.clearJWT()
        authUtil.signOut()
    }

    override suspend fun submitAccessTotpVerification(
        participantId: ParticipantId,
        verificationCode: String
    ): Resource<SubmitAccessTotpVerificationApiResponse> {
        val deviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())

        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()

        val dataToSign =
            verificationCode.toByteArray() + currentTimeInMillis.toString().toByteArray()
        val signature = deviceKey.sign(dataToSign).base64Encoded()

        return retrieveApiResource {
            apiService.submitAccessTotpVerification(
                participantId = participantId.value,
                apiRequest = SubmitAccessTotpVerificationApiRequest(
                    signature = signature,
                    timeMillis = currentTimeInMillis,
                    ownerDevicePublicKey = Base58EncodedDevicePublicKey(deviceKey.publicExternalRepresentation().value)
                )
            )
        }
    }

    override suspend fun retrieveAccessShards(
        biometryVerificationId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<RetrieveAccessShardsApiResponse> {

        return retrieveApiResource {
            apiService.retrieveAccessShards(
                apiRequest = RetrieveAccessShardsApiRequest(
                    biometryVerificationId = biometryVerificationId,
                    biometryData = biometryData
                )
            )
        }
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun recoverSeedPhrases(
        encryptedSeedPhrases: List<SeedPhrase>,
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        encryptedMasterPrivateKey: Base64EncodedData,
        language: BIP39.WordListLanguage?
    ): List<RecoveredSeedPhrase> {
        val intermediateEncryptionKey =
            recoverIntermediateEncryptionKey(encryptedIntermediatePrivateKeyShards, true)
        val masterEncryptionKey =
            recoverMasterEncryptionKey(encryptedMasterPrivateKey, intermediateEncryptionKey)

        return encryptedSeedPhrases.map {
            val response = retrieveSeedPhrase(it.guid)
            if (response is Resource.Success) {
                RecoveredSeedPhrase(
                    guid = it.guid,
                    label = it.label,
                    seedPhrase = masterEncryptionKey
                        .decrypt(response.data.encryptedSeedPhrase.bytes)
                        .toSeedPhraseData(language = language),
                    createdAt = it.createdAt
                )
            } else {
                throw (response as? Resource.Error)?.exception ?: Exception("Unknown failure")
            }
        }
    }

    private suspend fun retrieveSeedPhrase(seedPhraseId: SeedPhraseId): Resource<GetSeedPhraseApiResponse> {
        return retrieveApiResource {
            apiService.getSeedPhrase(seedPhraseId)
        }
    }

    override suspend fun submitPurchase(purchaseToken: String): Resource<SubmitPurchaseApiResponse> {
        return retrieveApiResource {
            apiService.submitPurchase(
                apiRequest = SubmitPurchaseApiRequest(
                    purchase = SubmitPurchaseApiRequest.Purchase.PlayStore(
                        purchaseToken = purchaseToken
                    )
                )
            )
        }
    }

    override suspend fun completeApproverOwnership(
        participantId: ParticipantId,
        completeOwnerApprovershipApiRequest: CompleteOwnerApprovershipApiRequest
    ): Resource<CompleteOwnerApprovershipApiResponse> {
        return retrieveApiResource {
            apiService.completeOwnerApprovership(
                participantId.value,
                completeOwnerApprovershipApiRequest
            )
        }
    }

    override suspend fun acceptImport(channel: String, ownerProof: OwnerProof): Resource<Unit> {
        return retrieveApiResource {
            apiService.acceptImport(
                channel = channel,
                ownerProof = ownerProof
            )
        }
    }

    override suspend fun checkForCompletedImport(channel: String): Resource<GetImportEncryptedDataApiResponse> {
        return retrieveApiResource {
            apiService.importedEncryptedData(channel)
        }
    }

    override suspend fun setPromoCode(code: String): Resource<Unit> {
        return retrieveApiResource {
            apiService.setPromoCode(SetPromoCodeApiRequest(code))
        }
    }

    override suspend fun inviteBeneficiary(
        label: String,
        deviceEncryptedTotpSecret: Base64EncodedData
    ): Resource<InviteBeneficiaryApiResponse> {
        return retrieveApiResource {
            apiService.inviteBeneficiary(
                InviteBeneficiaryApiRequest(
                    label = label,
                    deviceEncryptedTotpSecret = deviceEncryptedTotpSecret
                )
            )
        }
    }

    override suspend fun activateBeneficiary(
        ownerParticipantId: ParticipantId,
        entropy: Base64EncodedData,
        beneficiaryPublicKey: Base58EncodedBeneficiaryPublicKey,
        approverPublicKeys: List<ApproverPublicKey>,
    ): Resource<ActivateBeneficiaryApiResponse> {
        try {
            val ownerApproverKeyResource = keyRepository.retrieveKeyFromCloud(
                id = ownerParticipantId.value,
                bypassScopeCheck = true
            )

            if (ownerApproverKeyResource !is Resource.Success) {
                return if (ownerApproverKeyResource is Resource.Error) {
                    Resource.Error(
                        ownerApproverKeyResource.exception,
                        ownerApproverKeyResource.errorResponse,
                        ownerApproverKeyResource.errorCode
                    )
                } else {
                    Resource.Error(
                        Exception("Failed to retrieve owner key from cloud to activate beneficiary.")
                    )
                }
            }

            val encryptedKey = ownerApproverKeyResource.asSuccess().data

            val base58EncodedPrivateKey =
                encryptedKey.decryptWithEntropy(
                    deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                    entropy = entropy
                )

            val keyConfirmationTimeMillis = Clock.System.now().toEpochMilliseconds()

            val keyConfirmationMessage =
                beneficiaryPublicKey.getBytes() + keyConfirmationTimeMillis.toString().toByteArray()

            val encryptionKey =
                EncryptionKey.generateFromPrivateKeyRaw(base58EncodedPrivateKey.bigInt())

            val keyConfirmationSignature = encryptionKey.sign(keyConfirmationMessage)

            val beneficiaryKey =
                ExternalEncryptionKey.generateFromPublicKeyBase58(beneficiaryPublicKey)

            val encryptedOwnerKey = beneficiaryKey.encrypt(
                Base58.base58Decode(base58EncodedPrivateKey.value)
            )

            val beneficiaryKeys = approverPublicKeys.map {
                BeneficiaryEncryptedKey(
                    participantId = it.participantId,
                    encryptedKey = Base64EncodedData(
                        Base64.getEncoder().encodeToString(
                            ExternalEncryptionKey.generateFromPublicKeyBase58(it.publicKey)
                                .encrypt(encryptedOwnerKey)
                        )
                    )
                )
            }

            val activateBeneficiaryRequest = ActivateBeneficiaryApiRequest(
                keyConfirmationSignature =
                Base64EncodedData(
                    Base64.getEncoder().encodeToString(
                        keyConfirmationSignature
                    )
                ),
                keyConfirmationTimeMillis = keyConfirmationTimeMillis,
                encryptedKeys = beneficiaryKeys
            )

            return retrieveApiResource {
                apiService.activateBeneficiary(activateBeneficiaryRequest)
            }
        } catch (e: Exception) {
            return Resource.Error(e)
        }
    }

    override suspend fun rejectBeneficiaryVerification(): Resource<RejectBeneficiaryVerificationApiResponse> {
        return retrieveApiResource {
            apiService.rejectBeneficiaryVerification()
        }
    }

    private suspend fun recoverIntermediateEncryptionKey(
        encryptedIntermediatePrivateKeyShards: List<EncryptedShard>,
        bypassScopeCheck: Boolean = false
    ): PrivateKey {
        val ownerDeviceKey = InternalDeviceKey(secureStorage.retrieveDeviceKeyId())

        val intermediateKeyShares = encryptedIntermediatePrivateKeyShards.map {
            val encryptionKey = when (it.isOwnerShard) {
                true -> {
                    val ownerApproverKeyResource = keyRepository.retrieveKeyFromCloud(
                        id = it.participantId.value,
                        bypassScopeCheck = bypassScopeCheck
                    )

                    if (ownerApproverKeyResource is Resource.Error) {
                        throw ownerApproverKeyResource.exception!!
                    } else {
                        val encryptedKey = ownerApproverKeyResource.asSuccess().data
                        val base58EncodedPrivateKey =
                            encryptedKey.decryptWithEntropy(
                                deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                                entropy = it.ownerEntropy!!
                            )

                        EncryptionKey.generateFromPrivateKeyRaw(base58EncodedPrivateKey.bigInt())
                    }
                }

                false -> ownerDeviceKey
            }
            Point(
                it.participantId.bigInt(),
                BigInteger(1, encryptionKey.decrypt(it.encryptedShard.bytes))
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
            1,
            ECIESManager.decryptMessage(
                cipherData = encryptedMasterKey.bytes,
                privateKey = intermediateKey
            )
        )
    )
}
