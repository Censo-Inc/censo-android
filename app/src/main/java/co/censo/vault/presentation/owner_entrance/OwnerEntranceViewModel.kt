package co.censo.vault.presentation.owner_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.repository.BaseRepository.Companion.HTTP_401
import co.censo.vault.data.repository.MockUserState
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * General Android Owner Flow
 * Step 1: Check users saved timestamp
 *       - Do biometry to sign timestamp if needed
 * Step 2: Check User State: GET /user
 *      - If no user, then will create and verify contact
 *      - If need to complete Facetec action, verify user w/ Facetec
 * Step 3: Send user to list of BIP 39 phrases
 *
 * First Time Android Owner Flow:
 * Step 1: Check users saved timestamp
 *      - Do biometry to sign timestamp if needed
 * Step 2: Check User State: GET /user
 * Step 3: Receive 401
 *      - User input contact information (phone or email)
 *      - POST /user
 *      - Store returned verification id
 * Step 4: User input verification code -> POST /verifications/{id}/code
 *      - Not expecting data returned here. Looking for success.
 * Step 5: Check User State: GET /user
 * Step 6: Send user to Facetec enrollment/auth
 *      - *Not implemented yet*
 * Step 7: Owner is fully created
 *
 */

@HiltViewModel
class OwnerEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun onStart() {
        if (ownerRepository.checkValidTimestamp()) {
            vaultLog(message = "Have a valid timestamp moving forward...")
            checkUserState(MockUserState.NOT_FOUND)
        } else {
            vaultLog(message = "Timestamp missing or expired, triggering biometry...")
            triggerBiometryPrompt()
        }
    }

    private fun triggerBiometryPrompt() {
        state = state.copy(bioPromptTrigger = Resource.Success(Unit))
    }

    fun onBiometryApproved() {
        checkUserState(mockUserState = MockUserState.NOT_FOUND)
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    fun updateVerificationCode(value: String) {
        state = state.copy(
            verificationCode = value,
        )
    }

    fun updateContactValue(value: String) {
        state = state.copy(
            contactValue = value,
            validationError = "",
        )
    }

    fun showVerificationDialog() {
        state = state.copy(userStatus = UserStatus.VERIFY_CODE_ENTRY)
    }

    fun ownerAction(ownerAction: OwnerAction) {
        viewModelScope.launch {
            when (ownerAction) {
                is OwnerAction.EmailSubmitted -> {
                    state = state.copy(
                        createOwnerResource = Resource.Loading(),
                    )

                    val userEnterValidEmail = state.validateEmail(state.contactValue)

                    if (!userEnterValidEmail) {
                        state = state.copy(
                            createOwnerResource = Resource.Uninitialized,
                            validationError = "Please enter a valid email...",
                        )
                        return@launch
                    }

                    registerUserToBackend(
                        contactType = ContactType.Email,
                        value = state.contactValue
                    )

                }
                is OwnerAction.EmailVerification -> {
                    state = state.copy(
                        verificationResource = Resource.Loading()
                    )

                    val verifyEmailResponse = ownerRepository.verifyContact(
                        verificationId = state.verificationId,
                        verificationCode = state.verificationCode
                    )

                    state = state.copy(verificationResource = verifyEmailResponse)

                    if (verifyEmailResponse is Resource.Success) {
                        checkUserState(MockUserState.VERIFIED)
                    }

                    state = state.copy(verificationResource = Resource.Uninitialized)
                }
                is OwnerAction.PhoneSubmitted -> {
                    state = state.copy(
                        createOwnerResource = Resource.Loading(),
                    )

                    val userEnteredValidPhone =
                        state.validatePhone(state.contactValue)

                    if (!userEnteredValidPhone) {
                        state = state.copy(
                            createOwnerResource = Resource.Uninitialized,
                            validationError = "Please enter a valid phone...",
                        )
                        return@launch
                    }

                    registerUserToBackend(
                        contactType = ContactType.Phone,
                        value = state.contactValue
                    )
                }
                is OwnerAction.PhoneVerification -> {
                    state = state.copy(verificationResource = Resource.Loading())

                    val verifyPhoneResponse = ownerRepository.verifyContact(
                        verificationId = state.verificationId,
                        verificationCode = state.verificationCode
                    )

                    state = state.copy(verificationResource = verifyPhoneResponse)

                    if (verifyPhoneResponse is Resource.Success) {
                        checkUserState(MockUserState.VERIFIED)
                    }

                    state = state.copy(verificationResource = Resource.Uninitialized)
                }
            }
        }
    }

    private suspend fun registerUserToBackend(contactType: ContactType, value: String) {
        val createOwnerResponse = ownerRepository.createOwner(
            createUserApiRequest = CreateUserApiRequest(
                contactType = contactType,
                value = value
            )
        )

        if (createOwnerResponse is Resource.Success) {
            state = state.copy(
                verificationId = createOwnerResponse.data?.verificationId ?: "",
                userStatus = UserStatus.CONTACT_UNVERIFIED,
                createOwnerResource = createOwnerResponse
            )
        } else if (createOwnerResponse is Resource.Error) {
            state = state.copy(createOwnerResource = createOwnerResponse)
        }
    }

    private fun checkUserState(mockUserState: MockUserState) {
        state = state.copy(userResource = Resource.Loading())
        viewModelScope.launch {
            val user = ownerRepository.retrieveUser(mockUserState)
            vaultLog(message = "User coming back from retrieve user: ${user.data}")
            when (user) {
                is Resource.Error -> {
                    vaultLog(message = "Retrieve user failed")
                    state =
                        if (user.errorCode != null && user.errorCode == HTTP_401) {
                            vaultLog(message = "Received 401. User not created.")
                            state.copy(
                                userStatus = UserStatus.CREATE_CONTACT,
                                userResource = Resource.Uninitialized
                            )
                        } else {
                            vaultLog(message = "Failed to retrieve user")
                            state.copy(userResource = user)
                        }
                }

                is Resource.Success -> {
                    vaultLog(message = "Retrieve user success")
                    determineUserStatus(user.data)
                    state = state.copy(userResource = user)
                }

                else -> {
                    state = state.copy(userResource = user)
                }
            }
        }
    }

    private fun determineUserStatus(user: GetUserApiResponse?) {
        if (user == null) {
            state = state.copy(userStatus = UserStatus.UNINITIALIZED)
            return
        }

        if (user.contacts.isEmpty()) {
            state = state.copy(userStatus = UserStatus.CONTACT_UNVERIFIED)
            return
        }

        if (user.contacts.any { it.verified }) {
            state = state.copy(
                userStatus = UserStatus.COMPLETE_FACETEC,
                userFinishedSetup = Resource.Success(Screen.FacetecAuthRoute.route)
            )
            return
        }
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }
}