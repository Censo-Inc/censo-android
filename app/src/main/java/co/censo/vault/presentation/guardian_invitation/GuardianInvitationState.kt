package co.censo.vault.presentation.guardian_invitation

import android.util.Patterns
import co.censo.vault.data.Resource
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.GetUserApiResponse

data class ContactStateData(
    val value: String = "",
    val validationError: String = "",
    val creationResource: Resource<Unit> = Resource.Uninitialized,
    val verificationResource: Resource<Unit> = Resource.Uninitialized,
    val verificationCode: String = "",
    val verified : Boolean = false,
    val contactType: ContactType = ContactType.Email,
    val identifier : String = "",
)

data class GuardianInvitationState(
    val user: Resource<GetUserApiResponse?> = Resource.Uninitialized,
    val emailContactStateData: ContactStateData = ContactStateData(contactType = ContactType.Email),
    val phoneContactStateData: ContactStateData = ContactStateData(contactType = ContactType.Phone),
    val ownerState : OwnerState = OwnerState.NEW,
    val ownerName: String = "",
    val invitedGuardian: GuardianInformation = GuardianInformation(),
    val ownerInputState: OwnerInputState = OwnerInputState.VIEWING_CONTACTS,
    val isLoading: Boolean = false,
    val bioPromptTrigger: Resource<OwnerAction> = Resource.Uninitialized
) {

    fun emailContactState(): ContactState {
        val emailContact = user.data?.contacts?.firstOrNull { it.contactType == ContactType.Email }

        return if (emailContact == null) {
            ContactState.DOES_NOT_EXIST
        } else if (emailContact.verified) {
            ContactState.VERIFIED
        } else {
            ContactState.UNVERIFIED
        }
    }

    fun phoneContactState(): ContactState {
        val phoneContact = user.data?.contacts?.firstOrNull { it.contactType == ContactType.Phone }

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

enum class ContactState {
    DOES_NOT_EXIST, UNVERIFIED, VERIFIED
}

data class GuardianInformation(
    val name: String = "",
    val email: String = "",
    val verificationCode: String = "",
    val invitationStatus: GuardianInvitationStatus = GuardianInvitationStatus.PENDING
)

enum class GuardianInvitationStatus{
    PENDING, ACCEPTED, DECLINED
}

enum class OwnerState {
    NEW, VERIFYING, VERIFIED, GUARDIAN_INVITED
}

enum class OwnerInputState {
    VIEWING_CONTACTS,
    VERIFY_OWNER_EMAIL,
    VERIFY_OWNER_PHONE
}

sealed class OwnerAction {
    object EmailSubmitted : OwnerAction()
    object PhoneSubmitted : OwnerAction()
    object EmailVerification : OwnerAction()
    object PhoneVerification : OwnerAction()
    object NameSubmitted : OwnerAction()
}