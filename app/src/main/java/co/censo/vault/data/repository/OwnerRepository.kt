package co.censo.vault.data.repository

import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.Guardian
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.storage.Storage
import co.censo.vault.presentation.home.Screen.Companion.VAULT_GUARDIAN_URI
import kotlinx.datetime.Clock
import okhttp3.ResponseBody

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse>
    suspend fun createDevice(): Resource<ResponseBody>
    suspend fun createOwner(createUserApiRequest: CreateUserApiRequest): Resource<CreateUserApiResponse>
    suspend fun verifyContact(
        verificationId: String,
        verificationCode: String
    ): Resource<ResponseBody>

    fun checkValidTimestamp(): Boolean
    fun saveValidTimestamp()
    suspend fun createKeysAndShareInfo(guardians: List<Guardian>) : List<ShareInfo>

    suspend fun retrieveGuardianDeepLinks() : List<String>
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

    override suspend fun createDevice(): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.createDevice()
        }
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

    override suspend fun createKeysAndShareInfo(guardians: List<Guardian>) : List<ShareInfo> {

        //1. Create Master Encryption Key
        val masterEncryptKey = cryptographyManager.createMasterEncryptionKey()

        //2. Encrypt Master Encryption Key w/ Device Key
        val encryptedMasterKey = cryptographyManager.encryptData(masterEncryptKey)

        //3. Save Encrypted Master Key
        storage.saveMasterEncryptionKey(encryptedMasterKey)

        //4. Create Intermediate Key for Share
        val policyKey = cryptographyManager.createPolicyKey()

        //5. Encrypt Master Encryption Key w/ Intermediate Key
        //TODO

        //6. Send encrypted master key to Censo


        //7. Create and persist random coefficients and part ids for a shamir share of the intermediate key based on guardians list

        return guardians.map { ShareInfo(coefficient = it.name, participantId = it.email) }
    }

    override suspend fun retrieveGuardianDeepLinks(): List<String> {
        //1. Get list of shares saved in Shared Prefs
        val shares = listOf("1", "2", "3")

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

data class ShareInfo(
    val coefficient: String,
    val participantId: String
)
