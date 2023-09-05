package co.censo.vault.presentation.owner_entrance

import android.util.Patterns
import co.censo.vault.data.Resource
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateUserApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import okhttp3.ResponseBody

data class OwnerEntranceState(
    val createOwnerResource: Resource<CreateUserApiResponse> = Resource.Uninitialized,
    val userResource: Resource<GetUserApiResponse?> = Resource.Uninitialized,
    val verificationResource: Resource<ResponseBody> = Resource.Uninitialized,
    val verificationCode: String = "",
    val userStatus: UserStatus = UserStatus.UNINITIALIZED,
    val contactValue: String = "",
    val contactType: ContactType = ContactType.Email,
    val contactVerified: Boolean = false,
    val validationError: String = "",
    val verificationId: String = "",
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized
) {
    val isLoading =
        createOwnerResource is Resource.Loading || verificationResource is Resource.Loading || userResource is Resource.Loading

    val apiCallErrorOccurred =
        createOwnerResource is Resource.Error || verificationResource is Resource.Error || userResource is Resource.Error

    fun emailContactState(): ContactState {
        val emailContact =
            userResource.data?.contacts?.firstOrNull { it.contactType == ContactType.Email }

        return if (emailContact == null) {
            ContactState.DOES_NOT_EXIST
        } else if (emailContact.verified) {
            ContactState.VERIFIED
        } else {
            ContactState.UNVERIFIED
        }
    }

    fun phoneContactState(): ContactState {
        val phoneContact =
            userResource.data?.contacts?.firstOrNull { it.contactType == ContactType.Phone }

        return if (phoneContact == null) {
            ContactState.DOES_NOT_EXIST
        } else if (phoneContact.verified) {
            ContactState.VERIFIED
        } else {
            ContactState.UNVERIFIED
        }
    }

    fun validateEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun validatePhone(phone: String) = Patterns.PHONE.matcher(phone).matches()
}

enum class UserStatus {
    UNINITIALIZED, CREATE_CONTACT, CONTACT_UNVERIFIED, VERIFY_CODE_ENTRY
}

enum class ContactState {
    DOES_NOT_EXIST, UNVERIFIED, VERIFIED
}

sealed class OwnerAction {
    object EmailSubmitted : OwnerAction()
    object PhoneSubmitted : OwnerAction()
    object EmailVerification : OwnerAction()
    object PhoneVerification : OwnerAction()
    data class UpdateContact(val value: String) : OwnerAction()
    data class UpdateVerificationCode(val value: String) : OwnerAction()
    object ShowVerificationDialog : OwnerAction()
}