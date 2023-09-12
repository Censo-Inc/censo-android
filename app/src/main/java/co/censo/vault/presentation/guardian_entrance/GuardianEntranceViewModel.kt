package co.censo.vault.presentation.guardian_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.AcceptGuardianshipApiRequest
import co.censo.vault.data.model.GuardianPhase
import co.censo.vault.data.model.GuardianState
import co.censo.vault.data.repository.BaseRepository.Companion.HTTP_401
import co.censo.vault.data.repository.ErrorInfo
import co.censo.vault.data.repository.ErrorResponse
import co.censo.vault.data.repository.GuardianRepository
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianEntranceViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(GuardianEntranceState())
        private set

    fun onStart(args: GuardianEntranceArgs) {
        if (args.isDataMissing()) {
            vaultLog(message = "Arg data missing")
            state = state.copy(guardianStatus = GuardianStatus.DATA_MISSING)
            return
        }

        state = state.copy(
            participantId = args.participantId,
            ownerDevicePublicKey = args.ownerDevicePublicKey,
            intermediateKey = args.intermediateKey
        )

        if (ownerRepository.checkValidTimestamp()) {
            vaultLog(message = "Valid timestamp for auth headers, retrieving guardian status")
            retrieveGuardianStatus()
        } else {
            triggerBiometryPrompt(BiometryAuthReason.REGISTER_GUARDIAN)
        }
    }

    fun triggerBiometryPrompt(biometryAuthReason: BiometryAuthReason) {
        state = state.copy(bioPromptTrigger = Resource.Success(biometryAuthReason))
    }

    fun onBiometryApproved() {
        state = try {
            if (!ownerRepository.checkValidTimestamp()) {
                vaultLog(message = "Biometry approved, saving valid timestamp")
                ownerRepository.saveValidTimestamp()
            }

            state.bioPromptTrigger.data?.let {
                when (it) {
                    BiometryAuthReason.REGISTER_GUARDIAN -> {
                        vaultLog(message = "Biometry approved, registering guardian")
                        registerGuardian()
                    }
                    BiometryAuthReason.SUBMIT_VERIFICATION_CODE -> {
                        vaultLog(message = "Biometry approved, submitting verification code")
                        submitVerificationCode()
                    }
                }
            }

            state.copy(bioPromptTrigger = Resource.Uninitialized)
        } catch (e: Exception) {
            vaultLog(message = "Biometry approved but caught exception: $e")
            //TODO: Log exception with raygun
            state.copy(bioPromptTrigger = Resource.Error())

        }
    }

    fun onBiometryFailed() {
        vaultLog(message = "Biometry failed")
        state = state.copy(bioPromptTrigger = Resource.Error(data = state.bioPromptTrigger.data))
    }

    private fun retrieveGuardianStatus() {
        viewModelScope.launch {
            val getGuardianResource = guardianRepository.getGuardian(
                intermediateKey = state.intermediateKey,
                participantId = state.participantId,
            )

            if (getGuardianResource is Resource.Success) {
                setGuardianStatus(getGuardianResource.data?.guardianState)
            } else if (getGuardianResource is Resource.Error && getGuardianResource.errorCode == HTTP_401) {
                vaultLog(message = "Get guardian status returned 401, guardian has not registered device")
                state = state.copy(guardianStatus = GuardianStatus.REGISTER_GUARDIAN)
            }

            state = state.copy(getGuardianResource = getGuardianResource)
        }
    }

    fun setGuardianStatus(guardianState: GuardianState?) {
        if (guardianState == null) {
            state = state.copy(guardianStatus = GuardianStatus.DECLINED)
            return
        }

        val guardianStatus = when (guardianState.phase) {
            GuardianPhase.WaitingForCode -> {
                GuardianStatus.WAITING_FOR_CODE
            }
            GuardianPhase.WaitingForShard -> {
                //TODO: Mocked for now, backend will be updating and making the submitVerificationCode the last step for the guardian user
                GuardianStatus.COMPLETE
            }
            else -> {
                GuardianStatus.COMPLETE
            }
        }

        state = state.copy(
            participantId = guardianState.participantId,
            intermediateKey = guardianState.intermediatePublicKey,
            guardianStatus = guardianStatus
        )
    }

    fun registerGuardian() {
        viewModelScope.launch {
            val registerGuardianResponse = guardianRepository.registerGuardian(
                intermediateKey = state.intermediateKey,
                participantId = state.participantId
            )

            if (registerGuardianResponse is Resource.Success) {
                vaultLog(message = "Guardian Registered")
                vaultLog(message = "Response body: ${registerGuardianResponse.data}")
                setGuardianStatus(registerGuardianResponse.data?.guardianState)
            } else {
                vaultLog(message = "Guardian Registration failed")
            }

            state = state.copy(
                registerGuardianResource = registerGuardianResponse
            )
        }
    }

    fun declineGuardianship() {
        state = state.copy(declineGuardianshipResource = Resource.Loading())
        viewModelScope.launch {
            val declineGuardianshipResponse = guardianRepository.declineGuardianship(
                intermediateKey = state.intermediateKey, participantId = state.participantId
            )

            if (declineGuardianshipResponse is Resource.Success) {
                state = state.copy(guardianStatus = GuardianStatus.DECLINED)
            }

            state = state.copy(declineGuardianshipResource = declineGuardianshipResponse)
        }
    }

    fun updateVerificationCode(value: String) {
        state = state.copy(verificationCode = value)
    }

    fun submitVerificationCode() {
        try {
            state = state.copy(acceptGuardianshipResource = Resource.Loading())
            viewModelScope.launch {
                val signedDataAndTimeInMillis = guardianRepository.signVerificationCode(state.verificationCode)

                val acceptGuardianshipApiRequest = AcceptGuardianshipApiRequest(
                    signature = signedDataAndTimeInMillis.first,
                    timeMillis = signedDataAndTimeInMillis.second
                )

                val acceptGuardianshipResponse = guardianRepository.acceptGuardianship(
                    intermediateKey = state.intermediateKey,
                    participantId = state.participantId,
                    acceptGuardianshipApiRequest = acceptGuardianshipApiRequest
                )

                if (acceptGuardianshipResponse is Resource.Success) {
                    vaultLog(message = "Guardian accepted")
                    vaultLog(message = "Response body: ${acceptGuardianshipResponse.data}")
                    setGuardianStatus(acceptGuardianshipResponse.data?.guardianState)
                }

                state = state.copy(acceptGuardianshipResource = acceptGuardianshipResponse)
            }
        } catch (e: Exception) {
            //TODO: Log exception with raygun
            state = state.copy(
                acceptGuardianshipResource = Resource.Error(
                    errorResponse = ErrorResponse(
                        errors = listOf(
                            ErrorInfo(
                                reason = null,
                                message = null,
                                displayMessage = "Unable to prepare data for accepting guardianship, please try again"
                            )
                        )
                    ),
                    exception = e
                )
            )
        }
    }

    fun resetBioPromptTrigger() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

}