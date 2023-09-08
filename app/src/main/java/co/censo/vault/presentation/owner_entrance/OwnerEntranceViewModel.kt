package co.censo.vault.presentation.owner_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.ContactType
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.repository.BaseRepository.Companion.HTTP_404
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
            checkUserState()
        } else {
            vaultLog(message = "Timestamp missing or expired, triggering biometry...")
            triggerBiometryPrompt()
        }
    }

    private fun triggerBiometryPrompt() {
        state = state.copy(bioPromptTrigger = Resource.Success(Unit))
    }

    fun onBiometryApproved() {
        ownerRepository.saveValidTimestamp()

        checkUserState()
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    private fun updateVerificationCode(value: String) {
        state = state.copy(
            verificationCode = value,
        )
    }

    private fun updateContactValue(value: String) {
        state = state.copy(
            contactValue = value,
            validationError = "",
        )
    }

    private fun showVerificationDialog() {
        state = state.copy(userStatus = UserStatus.VERIFY_CODE_ENTRY)
    }

    fun ownerAction(ownerAction: OwnerAction) {
        viewModelScope.launch {
            when (ownerAction) {
                OwnerAction.EmailSubmitted -> {
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
                OwnerAction.EmailVerification -> {
                    state = state.copy(
                        verificationResource = Resource.Loading()
                    )

                    val verifyEmailResponse = ownerRepository.verifyContact(
                        verificationId = state.verificationId,
                        verificationCode = state.verificationCode
                    )

                    state = state.copy(verificationResource = verifyEmailResponse)

                    if (verifyEmailResponse is Resource.Success) {
                        checkUserState()
                    }
                }
                OwnerAction.PhoneSubmitted -> {
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
                OwnerAction.PhoneVerification -> {
                    state = state.copy(verificationResource = Resource.Loading())

                    val verifyPhoneResponse = ownerRepository.verifyContact(
                        verificationId = state.verificationId,
                        verificationCode = state.verificationCode
                    )

                    state = state.copy(verificationResource = verifyPhoneResponse)

                    if (verifyPhoneResponse is Resource.Success) {
                        checkUserState()
                    }
                }
                is OwnerAction.UpdateContact -> { updateContactValue(ownerAction.value) }
                is OwnerAction.UpdateVerificationCode -> { updateVerificationCode(ownerAction.value) }
                OwnerAction.ShowVerificationDialog -> { showVerificationDialog() }
            }
        }
    }

    private suspend fun registerUserToBackend(contactType: ContactType, value: String) {
        state = state.copy(contactType = contactType)

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

    private fun checkUserState() {
        state = state.copy(userResource = Resource.Loading())
        viewModelScope.launch {
            val user = ownerRepository.retrieveUser()
            vaultLog(message = "User coming back from retrieve user: ${user.data}")
            when (user) {
                is Resource.Error -> {
                    vaultLog(message = "Retrieve user failed")
                    state =
                        if (user.errorCode != null && user.errorCode == HTTP_404) {
                            vaultLog(message = "Received 404. User not created.")
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
                    if (user.data == null) {
                        state =
                            state.copy(userStatus = UserStatus.UNINITIALIZED, userResource = user)
                        return@launch
                    }

                    if (user.data.contacts.isEmpty()) {
                        state = state.copy(
                            userStatus = UserStatus.CONTACT_UNVERIFIED,
                            userResource = user
                        )
                        return@launch
                    }

                    if (user.data.contacts.any { it.verified }) {
                        val nextScreen =
                            if (user.data.biometricVerificationRequired) Screen.FacetecAuthRoute.route else Screen.HomeRoute.route

                        state =
                            state.copy(
                                userStatus = UserStatus.UNINITIALIZED,
                                userFinishedSetup = Resource.Success(nextScreen),
                                userResource = user
                            )
                        return@launch
                    }
                }

                else -> {
                    state = state.copy(userResource = user)
                }
            }
        }
    }

    fun retryCreateUser() {
        viewModelScope.launch {
            registerUserToBackend(contactType = state.contactType, value = state.contactValue)
        }
    }

    fun retryGetUser() {
        viewModelScope.launch {
            checkUserState()
        }
    }

    fun retryVerifyContact() {
        viewModelScope.launch {
            val verifyContactResponse = ownerRepository.verifyContact(
                verificationId = state.verificationId,
                verificationCode = state.verificationCode
            )

            state = state.copy(verificationResource = verifyContactResponse)

            if (verifyContactResponse is Resource.Success) {
                checkUserState()
                state = state.copy(verificationResource = Resource.Uninitialized)
            }
        }
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetVerificationResource() {
        state = state.copy(verificationResource = Resource.Uninitialized)
    }

    fun resetUserResource() {
        state = state.copy(userResource = Resource.Uninitialized)
    }

    fun resetCreateOwnerResource() {
        state = state.copy(createOwnerResource = Resource.Uninitialized)
    }
}