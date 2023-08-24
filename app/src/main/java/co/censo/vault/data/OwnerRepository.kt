package co.censo.vault.data

import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateContactApiRequest
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

    suspend fun retrieveUser(state: UserState): Resource<GetUserApiResponse?>
    suspend fun createOwner(name: String): Resource<Unit>
    suspend fun createContact(contact: Contact): Resource<ResponseBody?>
    suspend fun verifyContact(contactId: String, verificationCode: String): Resource<ResponseBody?>
}

class OwnerRepositoryImpl(private val apiService: ApiService) : OwnerRepository {
    override suspend fun retrieveUser(state: UserState): Resource<GetUserApiResponse?> {

        val userResponse = apiService.user()

        return if (userResponse.isSuccessful) {
            Resource.Success(userResponse.body())
        } else {
            Resource.Error()
        }
    }

    override suspend fun createOwner(name: String): Resource<Unit> {
        val createUserApiRequest = CreateUserApiRequest(name)
        val response = apiService.createUser(createUserApiRequest)

        return if (response.isSuccessful) {
            Resource.Success(Unit)
        } else {
            Resource.Error()
        }
    }

    override suspend fun createContact(contact: Contact): Resource<ResponseBody?> {
        val createContactApiRequest = CreateContactApiRequest(
            contactType = contact.contactType,
            value = contact.value
        )

        val response = apiService.createContact(createContactApiRequest)

        return if (response.isSuccessful) {
            Resource.Success(response.body())
        } else {
            Resource.Error()
        }
    }

    override suspend fun verifyContact(
        contactId: String,
        verificationCode: String
    ): Resource<ResponseBody?> {
        val verifyContactApiRequest = VerifyContactApiRequest(
            verificationCode = verificationCode
        )

        val response = apiService.verifyContact(
            contactId = contactId,
            verifyContactApiRequest = verifyContactApiRequest
        )

        return if (response.isSuccessful) {
            Resource.Success(response.body())
        } else {
            Resource.Error()
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