package co.censo.vault.data

import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateContactApiRequest
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService

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
    suspend fun createOwner(): Resource<Unit>
    suspend fun createContact(contact: Contact): Resource<Unit>
    suspend fun verifyContact(contactId: String, verificationCode: String): Resource<Unit>
}

class OwnerRepositoryImpl(private val apiService: ApiService) : OwnerRepository {
    override suspend fun retrieveUser(state: UserState): Resource<GetUserApiResponse?> {

        return when (state) {
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

    override suspend fun createOwner(): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun createContact(contact: Contact): Resource<Unit> {
        //todo: this would be passed to the API

        val createContactApiRequest = CreateContactApiRequest(
            contactType = contact.contactType,
            value = contact.value
        )

        return Resource.Success(Unit)
    }

    override suspend fun verifyContact(
        contactId: String,
        verificationCode: String
    ): Resource<Unit> {
        val verifyContactApiRequest = VerifyContactApiRequest(
            verificationCode = verificationCode
        )

//        apiService.verifyContact(
//            contactId = contactId,
//            verifyContactApiRequest = verifyContactApiRequest
//        )

        return Resource.Success(Unit)
    }
}