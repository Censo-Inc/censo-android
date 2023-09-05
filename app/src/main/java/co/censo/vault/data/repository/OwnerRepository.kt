package co.censo.vault.data.repository

import android.security.keystore.UserNotAuthenticatedException
import co.censo.vault.AuthHeadersState
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateContactApiRequest
import co.censo.vault.data.model.CreateContactApiResponse
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.storage.Storage
import co.censo.vault.util.vaultLog
import kotlinx.datetime.Clock
import okhttp3.ResponseBody

enum class UserState() {
    NEW,
    CREATED,
    EMAIL_SUBMITTED,
    PHONE_SUBMITTED,
    FULLY_VERIFIED,
    EMAIL_VERIFIED,
}

enum class MockUserState {
    NOT_FOUND, CREATED, VERIFIED
}

interface OwnerRepository {

    suspend fun retrieveUser(mockUserState: MockUserState): Resource<GetUserApiResponse?>
    suspend fun createDevice(): Resource<ResponseBody>
    suspend fun createOwner(createUserApiRequest: CreateUserApiRequest): Resource<CreateUserApiResponse>
    //suspend fun createContact(contact: Contact): Resource<CreateContactApiResponse?>
    suspend fun verifyContact(verificationId: String, verificationCode: String): Resource<ResponseBody>
    fun checkValidTimestamp() : Boolean
    fun saveValidTimestamp()
}

class OwnerRepositoryImpl(private val apiService: ApiService, private val storage: Storage, private val cryptographyManager: CryptographyManager) :
    OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(mockUserState: MockUserState): Resource<GetUserApiResponse?> {
        //return retrieveApiResource { apiService.user() }

        return when (mockUserState) {
            MockUserState.CREATED ->
                Resource.Success(
                    data =
                    GetUserApiResponse(
                        name = "Action Jackson",
                        contacts = emptyList()
                    )
                )

            MockUserState.NOT_FOUND -> Resource.Error(errorCode = 401)
            MockUserState.VERIFIED -> Resource.Success(
                data = GetUserApiResponse(
                    name = "Action Jackson",
                    contacts = listOf(
                        Contact(
                            identifier = "mock email contact",
                            contactType = ContactType.Email,
                            value = "sam@ok.com",
                            verified = true
                        )
                    )
                )
            )
        }
    }

    override suspend fun createDevice(): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.createDevice()
        }
    }

    override suspend fun createOwner(createUserApiRequest: CreateUserApiRequest): Resource<CreateUserApiResponse> {
        vaultLog(message = "Data sent to create user: $createUserApiRequest")
        return Resource.Success(data = CreateUserApiResponse(verificationId = "123456"))
        return retrieveApiResource { apiService.createUser(createUserApiRequest) }
    }

    override suspend fun verifyContact(
        verificationId: String,
        verificationCode: String
    ): Resource<ResponseBody> {
        val verifyContactApiRequest = VerifyContactApiRequest(
            verificationCode = verificationCode
        )

        return Resource.Success(
            ResponseBody.create(contentType = null, content = byteArrayOf())
        )

//        return retrieveApiResource {
//            apiService.verifyContact(
//                verificationId = verificationId,
//                verifyContactApiRequest = verifyContactApiRequest
//            )
//        }
    }

    override fun checkValidTimestamp() : Boolean {
        val now = Clock.System.now()
        val cachedHeaders = storage.retrieveReadHeaders()
        return !(cachedHeaders == null || cachedHeaders.isExpired(now))
    }

    override fun saveValidTimestamp() {
        try {
            val cachedReadCallHeaders = cryptographyManager.createAuthHeaders(Clock.System.now())
            storage.saveReadHeaders(cachedReadCallHeaders)
        } catch (e : Exception) {
            //TODO: Log exception with raygun
        }
    }

    fun mockedUserData(userState: UserState): Resource<GetUserApiResponse> {
        return when (userState) {
            UserState.NEW -> Resource.Success(null)
            UserState.CREATED -> Resource.Success(
                GetUserApiResponse(
                    name = "Sam",
                    contacts = emptyList()
                )
            )

            UserState.EMAIL_SUBMITTED -> Resource.Success(
                GetUserApiResponse(
                    name = "Sam",
                    contacts = listOf(
                        Contact(
                            identifier = "123",
                            contactType = ContactType.Email,
                            value = "sam@ok.com",
                            verified = false
                        )
                    )
                )
            )

            UserState.PHONE_SUBMITTED -> Resource.Success(
                GetUserApiResponse(
                    name = "Sam",
                    contacts = listOf(
                        Contact(
                            identifier = "123",
                            contactType = ContactType.Email,
                            value = "sam@ok.com",
                            verified = false
                        ),
                        Contact(
                            identifier = "456",
                            contactType = ContactType.Phone,
                            value = "7138293088",
                            verified = false
                        )
                    )
                )
            )

            UserState.EMAIL_VERIFIED -> Resource.Success(
                GetUserApiResponse(
                    name = "Sam",
                    contacts = listOf(
                        Contact(
                            identifier = "123",
                            contactType = ContactType.Email,
                            value = "sam@ok.com",
                            verified = true
                        ),
                        Contact(
                            identifier = "456",
                            contactType = ContactType.Phone,
                            value = "7138293088",
                            verified = false
                        )
                    )
                )
            )

            UserState.FULLY_VERIFIED -> Resource.Success(
                GetUserApiResponse(
                    name = "Sam",
                    contacts = listOf(
                        Contact(
                            identifier = "456",
                            contactType = ContactType.Phone,
                            value = "7138293088",
                            verified = true
                        ),
                        Contact(
                            identifier = "123",
                            contactType = ContactType.Email,
                            value = "sam@ok.com",
                            verified = true
                        )
                    )
                )
            )
        }
    }
}