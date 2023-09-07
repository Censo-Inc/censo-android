package co.censo.vault.data.repository

import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.storage.Storage
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
}