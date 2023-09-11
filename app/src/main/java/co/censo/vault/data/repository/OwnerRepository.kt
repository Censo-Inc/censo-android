package co.censo.vault.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import GuardianProspect
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.PolicySetupHelper
import co.censo.vault.data.cryptography.generatePartitionId
import co.censo.vault.data.model.CreatePolicyApiRequest
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.PolicyGuardian
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.storage.Storage
import co.censo.vault.presentation.home.Screen.Companion.VAULT_GUARDIAN_URI
import co.censo.vault.util.vaultLog
import kotlinx.datetime.Clock
import okhttp3.ResponseBody

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun createOwner(createUserApiRequest: CreateUserApiRequest): Resource<CreateUserApiResponse>
    suspend fun verifyContact(
        verificationId: String,
        verificationCode: String
    ): Resource<ResponseBody>

    fun checkValidTimestamp(): Boolean
    fun saveValidTimestamp()
    suspend fun setupPolicy(threshold: Int, guardians: List<String>) : PolicySetupHelper
    suspend fun retrieveGuardianDeepLinks(guardians: List<PolicyGuardian.ProspectGuardian>, policyKey: Base58EncodedIntermediatePublicKey) : List<String>
    suspend fun createPolicy(setupHelper: PolicySetupHelper) : Resource<ResponseBody>
}

class OwnerRepositoryImpl(
    private val apiService: ApiService,
    private val storage: Storage,
    private val cryptographyManager: CryptographyManager
) :
    OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(): Resource<GetUserApiResponse> {
        return retrieveApiResource { apiService.user() }
    }

    override suspend fun createOwner(createUserApiRequest: CreateUserApiRequest): Resource<CreateUserApiResponse> {
        return retrieveApiResource { apiService.createUser(createUserApiRequest) }
    }

    override suspend fun verifyContact(
        verificationId: String,
        verificationCode: String
    ): Resource<ResponseBody> {
        val verifyContactApiRequest = VerifyContactApiRequest(
            verificationCode = verificationCode
        )
        return retrieveApiResource {
            apiService.verifyContact(
                verificationId = verificationId,
                verifyContactApiRequest = verifyContactApiRequest
            )
        }
    }

    override fun checkValidTimestamp(): Boolean {
        val now = Clock.System.now()
        val cachedHeaders = storage.retrieveReadHeaders()
        return !(cachedHeaders == null || cachedHeaders.isExpired(now))
    }

    override fun saveValidTimestamp() {
        try {
            val cachedReadCallHeaders = cryptographyManager.createAuthHeaders(Clock.System.now())
            storage.saveReadHeaders(cachedReadCallHeaders)
        } catch (e: Exception) {
            //TODO: Log exception with raygun
        }
    }

    override suspend fun setupPolicy(threshold: Int, guardians: List<String>) : PolicySetupHelper {

        val guardianProspect = guardians.map {
            GuardianProspect(
                label = it,
                participantId = generatePartitionId()
            )
        }

        val policySetupHelper = PolicySetupHelper.create(
            threshold = threshold,
            guardians = guardianProspect,
            deviceKey = cryptographyManager.getOrCreateDeviceKey()
        ) {
            cryptographyManager.encryptData(String(it))
        }

        vaultLog(message = "Policy Setup Helper Created: $policySetupHelper")

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

    override suspend fun retrieveGuardianDeepLinks(
        guardians: List<PolicyGuardian.ProspectGuardian>,
        policyKey: Base58EncodedIntermediatePublicKey
    ): List<String> {
        val devicePublicKey = cryptographyManager.getDevicePublicKeyInBase58()

        //3. Create deep links from this: generateGuardianDeeplink
        return guardians.map {
            generateGuardianDeeplink(
                participantId = it.participantId.value,
                policyKey = policyKey,
                devicePublicKey = devicePublicKey
            )
        }
    }

    private fun generateGuardianDeeplink(participantId: String, policyKey: Base58EncodedIntermediatePublicKey, devicePublicKey: Base58EncodedDevicePublicKey): String {
        return "$VAULT_GUARDIAN_URI${policyKey.value}/${devicePublicKey.value}/$participantId"
    }
}