package co.censo.censo.presentation.accept_beneficiary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiRequest
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.BeneficiaryRepository
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AcceptBeneficiaryViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
) : ViewModel() {

    var state by mutableStateOf(AcceptBeneficiaryState())
        private set

    fun onStart(inviteId: String?) {
        val userLoggedIn = hasUserLoggedIn()

        state =
                //Have an invitation id and logged in, so skip to show facetec info
            if (!inviteId.isNullOrEmpty() && userLoggedIn) {
                state.copy(
                    invitationId = inviteId,
                    userLoggedIn = true,
                    acceptBeneficiaryUIState = AcceptBeneficiaryUIState.FacetecInfo
                )
                //If no invite id, we will show paste link
                //If invite id and logged out, we will show welcome beneficiary
            } else {
                state.copy(
                    invitationId = inviteId ?: "",
                    userLoggedIn = userLoggedIn
                )
            }
    }

    fun onContinue(pastedInfo: String?) {
        if (!state.userLoggedIn) {
            state = state.copy(navigateToSignIn = Resource.Success(Unit))
            return
        }

        //todo: Verify pasted link
        state = if (hasUserLoggedIn()) {
            state.copy(
                invitationId = pastedInfo ?: "",
                acceptBeneficiaryUIState = AcceptBeneficiaryUIState.FacetecInfo
            )
        } else {
            state.copy(
                invitationId = pastedInfo ?: "",
                navigateToSignIn = Resource.Success(Unit)
            )
        }
    }

    private fun hasUserLoggedIn() = ownerRepository.retrieveJWT().isNotEmpty()
    fun startFacetecScan() {
        state = state.copy(facetecInProgress = true)
    }

    fun stopFacetecScan() {
        state = state.copy(facetecInProgress = false)
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {

        return viewModelScope.async {

            val acceptBeneficiaryResponse =
                beneficiaryRepository.acceptBeneficiaryInvitation(
                    inviteId = state.invitationId,
                    requestBody = AcceptBeneficiaryInvitationApiRequest(
                        biometryVerificationId = verificationId,
                        biometryData = biometry
                    )
                )

            acceptBeneficiaryResponse.onSuccess {
                state = state.copy(navigateToBeneficiary = Resource.Success(Unit))
            }

            state = state.copy(acceptBeneficiaryResponse = acceptBeneficiaryResponse)

            acceptBeneficiaryResponse.map { it.scanResultBlob }
        }.await()
    }

    fun deleteUser() {
        state = state.copy(deleteUserResource = Resource.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(null)

            response.onSuccess {
                state = state.copy(
                    navigateToSignIn = Resource.Success(Unit)
                )
            }

            state = state.copy(deleteUserResource = response)
        }
    }

    fun resetNavigation() {
        state = state.copy(
            navigateToSignIn = Resource.Uninitialized,
            navigateToBeneficiary = Resource.Uninitialized
        )
    }

    fun resetError() {
        state = state.copy(acceptBeneficiaryResponse = Resource.Uninitialized)
    }

    fun onCancelResetUser() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun showDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Success(Unit))
    }
}