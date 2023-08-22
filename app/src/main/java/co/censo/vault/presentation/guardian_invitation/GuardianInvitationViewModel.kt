package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.OwnerRepository
import co.censo.vault.data.Resource
import co.censo.vault.data.UserState
import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class GuardianInvitationViewModel @Inject constructor(private val ownerRepository: OwnerRepository) :
    ViewModel() {

    var state by mutableStateOf(GuardianInvitationState())
        private set

    fun onStart() {
        viewModelScope.launch {
            determineUserState(UserState.NEW)
        }
    }

    private suspend fun determineUserState(userState: UserState) {
        val user = ownerRepository.retrieveUser(userState)

        if (user is Resource.Success) {
            val userData = user.data

            state = if (userData == null) {
                state.copy(
                    user = user,
                    ownerState = OwnerState.NEW
                )
            } else {

                if (userData.contacts.isNullOrEmpty()) {
                    state.copy(
                        user = user,
                        ownerState = OwnerState.VERIFYING,
                        ownerInputState = OwnerInputState.VIEWING_CONTACTS
                    )
                } else {
                    val phoneContact =
                        userData.contacts.firstOrNull { it.contactType == ContactType.Phone }
                    val emailContact =
                        userData.contacts.firstOrNull { it.contactType == ContactType.Email }

                    if (emailContact == null || phoneContact == null) {
                        state.copy(
                            user = user,
                            ownerState = OwnerState.VERIFYING,
                            ownerInputState = OwnerInputState.VIEWING_CONTACTS
                        )
                    } else {
                        if (emailContact.verified && phoneContact.verified) {
                            state.copy(
                                user = user,
                                ownerState = OwnerState.VERIFIED,
                                phoneContactStateData = state.phoneContactStateData.copy(
                                    verified = true
                                ),
                                emailContactStateData = state.emailContactStateData.copy(
                                    verified = true
                                ),
                                ownerInputState = OwnerInputState.VIEWING_CONTACTS
                            )
                        } else {
                            state.copy(
                                user = user,
                                ownerState = OwnerState.VERIFYING,
                                phoneContactStateData = state.phoneContactStateData.copy(
                                    verified = phoneContact.verified
                                ),
                                emailContactStateData = state.emailContactStateData.copy(
                                    verified = emailContact.verified
                                ),
                                ownerInputState = OwnerInputState.VIEWING_CONTACTS
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateOwnerName(value: String) {
        state = state.copy(ownerName = value)
    }

    fun updateOwnerEmail(value: String) {
        state = state.copy(
            emailContactStateData = state.emailContactStateData.copy(
                value = value,
                validationError = ""
            )
        )
    }

    fun updateOwnerPhone(value: String) {
        state = state.copy(
            phoneContactStateData = state.phoneContactStateData.copy(
                value = value,
                validationError = ""
            ),
        )
    }

    fun ownerAction(ownerAction: OwnerAction) {
        viewModelScope.launch {
            when (ownerAction) {
                is OwnerAction.NameSubmitted -> {
                    val createOwnerResponse = ownerRepository.createOwner()

                    if (createOwnerResponse is Resource.Success) {
                        state = state.copy(ownerState = OwnerState.VERIFYING)
                    }
                }
                is OwnerAction.EmailSubmitted -> {
                    val userEnterValidEmail = state.validateEmail(state.emailContactStateData.value)

                    if (!userEnterValidEmail) {
                        state = state.copy(
                            emailContactStateData = state.emailContactStateData.copy(
                                validationError = "Please enter a valid email...",
                            ),
                        )
                        return@launch
                    }

                    val createContactResponse = ownerRepository.createContact(
                        Contact(
                            identifier = "",
                            contactType = ContactType.Email,
                            value = state.emailContactStateData.value,
                            verified = false
                        )
                    )

                    when (createContactResponse) {
                        is Resource.Success -> determineUserState(UserState.EMAIL_SUBMITTED)

                        is Resource.Error -> {
                            //todo: alert user they failed to submit email
                        }

                        else -> {}
                    }
                }
                is OwnerAction.EmailVerification -> {
                    val emailContactId = state.user.data?.contacts?.firstOrNull { it.contactType == ContactType.Email }?.identifier
                        ?: //todo: alert user they need to submit phone
                        return@launch

                    val verifyEmailResponse = ownerRepository.verifyContact(
                        contactId = emailContactId,
                        verificationCode = state.emailContactStateData.verificationCode
                    )

                    when (verifyEmailResponse) {
                        is Resource.Success -> {
                            determineUserState(UserState.EMAIL_VERIFIED)
                        }
                        is Resource.Error -> {
                            //todo: alert user they failed to verify phone
                        }
                        else -> {}
                    }
                }
                is OwnerAction.PhoneSubmitted -> {
                    val userEnteredValidPhone = state.validatePhone(state.phoneContactStateData.value)

                    if (!userEnteredValidPhone) {
                        state = state.copy(
                            phoneContactStateData = state.phoneContactStateData.copy(
                                validationError = "Please enter a valid phone...",
                            ),
                        )
                        return@launch
                    }

                    val createContactResponse = ownerRepository.createContact(
                        Contact(
                            identifier = "",
                            contactType = ContactType.Phone,
                            value = state.emailContactStateData.value,
                            verified = false
                        )
                    )

                    when (createContactResponse) {
                        is Resource.Success -> determineUserState(UserState.PHONE_SUBMITTED)

                        is Resource.Error -> {
                            //todo: alert user they failed to submit phone
                        }

                        else -> {}
                    }
                }
                is OwnerAction.PhoneVerification -> {
                    val phoneContactId = state.user.data?.contacts?.firstOrNull { it.contactType == ContactType.Phone }?.identifier
                        ?: //todo: alert user they need to submit phone
                        return@launch

                    val verifyPhoneResponse = ownerRepository.verifyContact(
                        contactId = phoneContactId,
                        verificationCode = state.phoneContactStateData.verificationCode
                    )

                    when (verifyPhoneResponse) {
                        is Resource.Success -> {
                            determineUserState(UserState.FULLY_VERIFIED)
                        }
                        is Resource.Error -> {
                            //todo: alert user they failed to verify phone
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun updateEmailVerificationCode(value: String) {
        state = state.copy(
            emailContactStateData = state.emailContactStateData.copy(verificationCode = value)
        )
    }

    fun updatePhoneVerificationCode(value: String) {
        state = state.copy(
            phoneContactStateData = state.phoneContactStateData.copy(verificationCode = value)
        )
    }

    fun sendVerificationCodesToOwner() {
        vaultLog(message = "Sending codes: \nemail: 123456 \nphone: 654321")
    }

    fun verifyOwnerEmail() {
        state = state.copy(
            emailContactStateData = state.emailContactStateData.copy(verificationCode = ""),
            ownerInputState = OwnerInputState.VERIFY_OWNER_EMAIL)
    }

    fun verifyOwnerPhone() {
        state = state.copy(
            phoneContactStateData = state.phoneContactStateData.copy(verificationCode = ""),
            ownerInputState = OwnerInputState.VERIFY_OWNER_PHONE
        )
    }

    fun submitGuardian(guardianEmail: String, guardianName: String) {
        if (guardianName.isNotEmpty() && guardianEmail.isNotEmpty()) {
            vaultLog(message = "Submitting guardian data to backend")
            //TODO: Make call to backend to submit guardian
            // Create intermediate key and compute share

            //Mocked for now
            viewModelScope.launch {
                state = state.copy(isLoading = true)
                delay(2500)

                val rng = Random
                val verificationCode = rng.nextInt(999999)

                state = state.copy(
                    invitedGuardian = GuardianInformation(
                        name = guardianName,
                        email = guardianEmail,
                        verificationCode = String.format("%06d", verificationCode)
                    ),
                    ownerState = OwnerState.GUARDIAN_INVITED,
                    isLoading = false
                )
            }
        }
    }
}

const val MAIN_PREFS = "CensoVault"

const val OWNER_VERIFIED_FLAG = "owner_verified"