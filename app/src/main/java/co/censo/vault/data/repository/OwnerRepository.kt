package co.censo.vault.data.repository

import GuardianProspect
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.PolicySetupHelper
import co.censo.vault.data.cryptography.generatePartitionId
import co.censo.vault.data.model.CreatePolicyApiRequest
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
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
    suspend fun retrieveGuardianDeepLinks(guardians: List<String>) : List<String>
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
            intermediateKey = setupHelper.intermediatePublicKey,
            guardiansToInvite = setupHelper.guardianInvites,
            threshold = setupHelper.threshold,
            encryptedData = setupHelper.encryptedMasterKey
        )

        return retrieveApiResource { apiService.createPolicy(createPolicyApiRequest) }
    }

    override suspend fun retrieveGuardianDeepLinks(guardians: List<String>): List<String> {
        //1. Get list of shares saved in Shared Prefs
        val shares = guardians.map { it }

        //2. Get public keys saved in shared prefs
        val policyKey = cryptographyManager.createPolicyKey()
        val devicePublicKey = cryptographyManager.getDevicePublicKeyInBase58()

        //3. Create deep links from this: generateGuardianDeeplink
        return shares.map {
            generateGuardianDeeplink(
                participantId = it,
                policyKey = policyKey,
                devicePublicKey = devicePublicKey
            )
        }
    }

    private fun generateGuardianDeeplink(participantId: String, policyKey: String, devicePublicKey: String): String {
        return "$VAULT_GUARDIAN_URI$policyKey/$devicePublicKey/$participantId"
    }
}