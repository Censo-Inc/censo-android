package co.censo.vault.data.repository

import co.censo.vault.data.Resource
import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateContactApiRequest
import co.censo.vault.data.model.CreateContactApiResponse
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService
import okhttp3.ResponseBody

enum class UserState() {
    NEW,
    CREATED,
    EMAIL_SUBMITTED,
    PHONE_SUBMITTED,
    FULLY_VERIFIED,
    EMAIL_VERIFIED,
}

interface OwnerRepository {

    suspend fun retrieveUser(): Resource<GetUserApiResponse?>
    suspend fun createDevice(): Resource<ResponseBody>
    suspend fun createOwner(name: String): Resource<ResponseBody>
    suspend fun createContact(contact: Contact): Resource<CreateContactApiResponse?>
    suspend fun verifyContact(verificationId: String, verificationCode: String): Resource<ResponseBody?>
}

class OwnerRepositoryImpl(private val apiService: ApiService) : OwnerRepository, BaseRepository() {
    override suspend fun retrieveUser(): Resource<GetUserApiResponse?> =
        retrieveApiResource { apiService.user() }

    override suspend fun createDevice(): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.createDevice()
        }
    }

    override suspend fun createOwner(name: String): Resource<ResponseBody> {
        val createUserApiRequest = CreateUserApiRequest(name)

        return retrieveApiResource { apiService.createUser(createUserApiRequest) }
    }

    override suspend fun createContact(contact: Contact): Resource<CreateContactApiResponse?> {
        val createContactApiRequest = CreateContactApiRequest(
            contactType = contact.contactType,
            value = contact.value
        )

        return retrieveApiResource { apiService.createContact(createContactApiRequest) }
    }

    override suspend fun verifyContact(
        verificationId: String,
        verificationCode: String
    ): Resource<ResponseBody?> {
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