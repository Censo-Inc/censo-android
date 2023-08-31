package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.data.repository.BaseRepository.Companion.AUTH_ERROR_REASON
import co.censo.vault.data.repository.BaseRepository.Companion.UNKNOWN_DEVICE_MESSAGE
import co.censo.vault.data.Resource
import co.censo.vault.data.model.Contact
import co.censo.vault.data.model.ContactType
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
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
            determineUserState()
        }
    }

    fun triggerBiometryPrompt(ownerAction: OwnerAction) {
        state = state.copy(bioPromptTrigger = Resource.Success(ownerAction))
    }

    fun onBiometryApproved(ownerAction: OwnerAction) {
        ownerAction(ownerAction = ownerAction, biometryApproved = true)
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    private suspend fun determineUserState() {
        state = state.copy(user = Resource.Loading())

        when (val user = ownerRepository.retrieveUser()) {
            is Resource.Error -> {
                val reason = user.errorResponse?.errors?.get(0)?.reason == AUTH_ERROR_REASON
                val message = user.errorResponse?.errors?.get(0)?.message == UNKNOWN_DEVICE_MESSAGE
                if (reason && message) {
                    createDevice()
                } else {
                    state = state.copy(
                        user = Resource.Uninitialized,
                        showToast = Resource.Success(user)
                    )
                }
            }
            is Resource.Success -> {
                val userData = user.data

                state = if (userData == null) {
                    state.copy(
                        user = user,
                        ownerState = OwnerState.NEW
                    )
                } else {

                    if (userData.contacts.isEmpty()) {
                        state.copy(
                            user = user,
                            ownerName = user.data.name,
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
                                ownerName = user.data.name,
                                ownerState = OwnerState.VERIFYING,
                                ownerInputState = OwnerInputState.VIEWING_CONTACTS,
                                phoneContactStateData = state.phoneContactStateData.copy(
                                    verified = phoneContact?.verified ?: false,
                                    value = phoneContact?.value ?: ""
                                ),
                                emailContactStateData = state.emailContactStateData.copy(
                                    verified = emailContact?.verified ?: false,
                                    value = emailContact?.value ?: ""
                                ),
                            )
                        } else {
                            if (emailContact.verified && phoneContact.verified) {
                                state.copy(
                                    user = user,
                                    ownerName = user.data.name,
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
                                    ownerName = user.data.name,
                                    ownerState = OwnerState.VERIFYING,
                                    phoneContactStateData = state.phoneContactStateData.copy(
                                        verified = phoneContact.verified,
                                        value = phoneContact.value
                                    ),
                                    emailContactStateData = state.emailContactStateData.copy(
                                        verified = emailContact.verified,
                                        value = emailContact.value
                                    ),
                                    ownerInputState = OwnerInputState.VIEWING_CONTACTS
                                )
                            }
                        }
                    }
                }
            }
            else -> { }
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

    fun ownerAction(ownerAction: OwnerAction, biometryApproved: Boolean = false) {

        if (!biometryApproved) {
            triggerBiometryPrompt(ownerAction)
            return
        }

        viewModelScope.launch {
            when (ownerAction) {
                //POST
                is OwnerAction.NameSubmitted -> {
                    state = state.copy(createOwnerResource = Resource.Loading())

                    val createOwnerResponse = ownerRepository.createOwner(state.ownerName)

                    if (createOwnerResponse is Resource.Success) {
                        state = state.copy(ownerState = OwnerState.VERIFYING)
                    } else if (createOwnerResponse is Resource.Error) {
                        state = state.copy(
                            showToast = Resource.Success(
                                createOwnerResponse
                            )
                        )
                    }

                    state = state.copy(createOwnerResource = Resource.Uninitialized)
                }
                //POST
                is OwnerAction.EmailSubmitted -> {
                    state = state.copy(
                        emailContactStateData = state.emailContactStateData.copy(
                            creationResource = Resource.Loading()
                        )
                    )

                    val userEnterValidEmail = state.validateEmail(state.emailContactStateData.value)

                    if (!userEnterValidEmail) {
                        state = state.copy(
                            emailContactStateData = state.emailContactStateData.copy(
                                validationError = "Please enter a valid email...",
                                creationResource = Resource.Uninitialized
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
                        is Resource.Success -> {
                            state = state.copy(emailContactStateData = state.emailContactStateData.copy(
                                verificationId = createContactResponse.data!!.verificationId
                            ))
                            determineUserState()
                        }

                        is Resource.Error -> {
                            state = state.copy(showToast = Resource.Success(createContactResponse))
                        }

                        else -> {}
                    }

                    state = state.copy(
                        emailContactStateData = state.emailContactStateData.copy(
                            creationResource = Resource.Uninitialized
                        ),
                    )
                }
                //POST
                is OwnerAction.EmailVerification -> {
                    val emailContactId = state.user.data?.contacts?.firstOrNull { it.contactType == ContactType.Email }?.identifier
                        ?: //todo: alert user they need to submit phone
                        return@launch

                    state = state.copy(
                        emailContactStateData = state.emailContactStateData.copy(
                            verificationResource = Resource.Loading()
                        ),
                    )

                    val verifyEmailResponse = ownerRepository.verifyContact(
                        verificationId = state.emailContactStateData.verificationId,
                        verificationCode = state.emailContactStateData.verificationCode
                    )

                    when (verifyEmailResponse) {
                        is Resource.Success -> {
                            determineUserState()
                        }
                        is Resource.Error -> {
                            state = state.copy(showToast = Resource.Success(
                                verifyEmailResponse
                            ))
                        }
                        else -> {}
                    }

                    state = state.copy(
                        emailContactStateData = state.emailContactStateData.copy(
                            verificationResource = Resource.Uninitialized
                        ),
                    )
                }
                //POST
                is OwnerAction.PhoneSubmitted -> {
                    state = state.copy(
                        phoneContactStateData = state.phoneContactStateData.copy(
                            creationResource = Resource.Loading()
                        ),
                    )

                    val userEnteredValidPhone = state.validatePhone(state.phoneContactStateData.value)

                    if (!userEnteredValidPhone) {
                        state = state.copy(
                            phoneContactStateData = state.phoneContactStateData.copy(
                                validationError = "Please enter a valid phone...",
                                creationResource = Resource.Uninitialized
                            ),
                        )
                        return@launch
                    }

                    val createContactResponse = ownerRepository.createContact(
                        Contact(
                            identifier = "",
                            contactType = ContactType.Phone,
                            value = state.phoneContactStateData.value,
                            verified = false
                        )
                    )

                    when (createContactResponse) {
                        is Resource.Success -> {
                            state = state.copy(phoneContactStateData = state.phoneContactStateData.copy(
                                verificationId = createContactResponse.data!!.verificationId
                            ))
                            determineUserState()
                        }

                        is Resource.Error -> {
                            state = state.copy(showToast = Resource.Success(
                                createContactResponse
                            ))
                        }

                        else -> {}
                    }

                    state = state.copy(
                        phoneContactStateData = state.phoneContactStateData.copy(
                            creationResource = Resource.Uninitialized
                        ),
                    )
                }
                //POST
                is OwnerAction.PhoneVerification -> {
                    val phoneContactId = state.user.data?.contacts?.firstOrNull { it.contactType == ContactType.Phone }?.identifier
                        ?: //todo: alert user they need to submit phone
                        return@launch

                    state = state.copy(
                        phoneContactStateData = state.phoneContactStateData.copy(
                            verificationResource = Resource.Loading()
                        ),
                    )

                    val verifyPhoneResponse = ownerRepository.verifyContact(
                        verificationId = state.phoneContactStateData.verificationId,
                        verificationCode = state.phoneContactStateData.verificationCode
                    )

                    when (verifyPhoneResponse) {
                        is Resource.Success -> {
                            determineUserState()
                        }
                        is Resource.Error -> {
                            state = state.copy(showToast = Resource.Success(
                                verifyPhoneResponse
                            ))
                        }
                        else -> {}
                    }

                    state = state.copy(
                        phoneContactStateData = state.phoneContactStateData.copy(
                            creationResource = Resource.Uninitialized
                        ),
                    )
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
                val rng = Random
                val verificationCode = rng.nextInt(999999)

                state = state.copy(
                    invitedGuardian = GuardianInformation(
                        name = guardianName,
                        email = guardianEmail,
                        verificationCode = String.format("%06d", verificationCode)
                    ),
                    ownerState = OwnerState.GUARDIAN_INVITED,
                )
            }
        }
    }

    private suspend fun createDevice() {
        val createDeviceResponse = ownerRepository.createDevice()

        if (createDeviceResponse is Resource.Success) {
            determineUserState()
        } else if (createDeviceResponse is Resource.Error) {
            state = state.copy(
                showToast = Resource.Success(
                    createDeviceResponse
                )
            )
        }
    }

    fun resetShowToast() {
        state = state.copy(showToast = Resource.Uninitialized)
    }
}