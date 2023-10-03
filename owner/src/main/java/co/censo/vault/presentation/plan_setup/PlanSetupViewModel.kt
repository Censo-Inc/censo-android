package co.censo.vault.presentation.plan_setup

import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.generateBase32
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.SecurityPlanData
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import java.util.Base64
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart(existingSecurityPlanData: SecurityPlanData?) {

        state = state.copy(existingSecurityPlan = existingSecurityPlanData)

        if (ownerRepository.isUserEditingSecurityPlan()) {
            val securityPlanData = ownerRepository.retrieveSecurityPlan()

            securityPlanData?.let {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.Review,
                    threshold = it.threshold,
                    guardians = it.guardians
                )
            }
        }
    }

    suspend fun onPolicySetupCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        return viewModelScope.async {
            val createPolicySetupResponse = ownerRepository.createPolicySetup(
                state.threshold,
                state.guardians.map {
                    Guardian.SetupGuardian(
                        label = it.label,
                        participantId = it.participantId,
                        deviceEncryptedTotpSecret = it.deviceEncryptedTotpSecret
                    )
                },
                verificationId,
                facetecData
            )

            if (createPolicySetupResponse is Resource.Success) {
                clearEditingPlanData()

                state = state.copy(
                    createPolicySetupResponse = createPolicySetupResponse,
                    navigateToActivateApprovers = true
                )
            } else if (createPolicySetupResponse is Resource.Error) {
                state = state.copy(
                    createPolicySetupResponse = createPolicySetupResponse,
                    currentScreen = SetupSecurityPlanScreen.SecureYourPlan
                )
            }

            createPolicySetupResponse.map { it.scanResultBlob }
        }.await()
    }

    fun onBackActionClick() {
        state = when (state.currentScreen) {
            SetupSecurityPlanScreen.RequiredApprovals -> {
                state.copy(
                    currentScreen = SetupSecurityPlanScreen.AddApprovers
                )
            }

            SetupSecurityPlanScreen.SecureYourPlan -> {
                state.copy(
                    currentScreen = SetupSecurityPlanScreen.RequiredApprovals
                )
            }

            else -> state
        }
    }

    fun onMainActionClick() {
        when (state.currentScreen) {
            SetupSecurityPlanScreen.Initial -> showAddGuardianDialog()

            SetupSecurityPlanScreen.AddApprovers -> {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.RequiredApprovals
                )
            }

            SetupSecurityPlanScreen.RequiredApprovals -> {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.Review
                )
            }

            SetupSecurityPlanScreen.Review -> {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.SecureYourPlan
                )
            }

            SetupSecurityPlanScreen.SecureYourPlan -> {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.FacetecAuth
                )
            }

            SetupSecurityPlanScreen.FacetecAuth -> {
                state = state.copy(
                    navigateToActivateApprovers = true
                )
            }
        }
    }

    fun showAddGuardianDialog() {
        state = state.copy(showAddGuardianDialog = true)
    }

    fun updateSliderPosition(updatedPosition: Float) {
        val updatedPositionAsUInt = updatedPosition.roundToInt().toUInt()

        if (updatedPositionAsUInt != state.threshold) {
            state = state.copy(
                threshold = updatedPositionAsUInt
            )
            saveCurrentPlan()
        }
    }

    fun onMoreInfoClicked() {

    }

    fun showEditOrDeleteDialog(guardian: Guardian.SetupGuardian) {
        state = state.copy(
            showEditOrDeleteDialog = true,
            editingGuardian = guardian
        )
    }

    fun editGuardian() {
        state.editingGuardian?.let {
            state = state.copy(
                showEditOrDeleteDialog = false,
                addedApproverNickname = it.label,
                showAddGuardianDialog = true,
                editingGuardian = it,
            )
        }
    }

    fun deleteGuardian() {
        state.editingGuardian?.let {
            val guardians = state.guardians.toMutableList()

            guardians.remove(it)

            val threshold = recalculateThreshold(
                previousGuardianSize = state.guardians.size,
                currentGuardianSize = guardians.size,
                threshold = state.threshold
            )

            state = state.copy(
                editingGuardian = null,
                guardians = guardians,
                showEditOrDeleteDialog = false,
                threshold = threshold
            )

            saveCurrentPlan()
        }
    }

    fun submitNewApprover() {
        val guardians = state.guardians.toMutableList()

        if (state.editingGuardian != null) {
            val index = guardians.indexOf(state.editingGuardian!!)

            if (index != -1) {
                guardians[index] = Guardian.SetupGuardian(
                    label = state.addedApproverNickname,
                    participantId = state.editingGuardian!!.participantId,
                    deviceEncryptedTotpSecret = state.editingGuardian!!.deviceEncryptedTotpSecret
                )
            }
        } else {
            val secret = TotpGenerator.generateSecret()
            val encryptedSecret = keyRepository.encryptWithDeviceKey(secret.toByteArray())

            guardians.add(
                Guardian.SetupGuardian(
                    label = state.addedApproverNickname,
                    participantId = ParticipantId(
                        generatePartitionId().toHexString()
                    ),
                    deviceEncryptedTotpSecret = Base64EncodedData(
                        Base64.getEncoder().encodeToString(encryptedSecret)
                    )
                )
            )
        }

        val screen = if (state.currentScreen == SetupSecurityPlanScreen.Initial) {
            SetupSecurityPlanScreen.AddApprovers
        } else {
            state.currentScreen
        }

        state = state.copy(
            addedApproverNickname = "",
            editingGuardian = null,
            guardians = guardians.toList(),
            showAddGuardianDialog = false,
            showEditOrDeleteDialog = false,
            currentScreen = screen
        )

        saveCurrentPlan()
    }

    fun updateAddedApproverNickname(updatedNickname: String) {
        state = state.copy(addedApproverNickname = updatedNickname)
    }

    fun dismissDialog() {
        state = state.copy(
            addedApproverNickname = "",
            editingGuardian = null,
            showAddGuardianDialog = false,
            showEditOrDeleteDialog = false,
            showCancelPlanSetupDialog = false
        )
    }

    fun resetCreatePolicySetup() {
        state = state.copy(
            createPolicySetupResponse = Resource.Uninitialized
        )
    }

    fun resetNavToActivateApprovers() {
        state = state.copy(navigateToActivateApprovers = false)
    }

    fun retryFacetec() {
        state = state.copy(
            createPolicySetupResponse = Resource.Uninitialized
        )

        onMainActionClick()
    }

    private fun recalculateThreshold(
        previousGuardianSize: Int,
        currentGuardianSize: Int,
        threshold: UInt
    ): UInt {
        val difference = previousGuardianSize - threshold.toInt()
        val newThreshold = currentGuardianSize - difference

        return if (newThreshold >= 1) newThreshold.toUInt() else 1u
    }

    fun clearEditingPlanData() {
        ownerRepository.setEditingSecurityPlan(false)
        ownerRepository.clearSecurityPlanData()
    }

    fun showCancelDialog() {
        val editedSecurityPlanData = SecurityPlanData(
            guardians = state.guardians,
            threshold = state.threshold
        )

        if (editedSecurityPlanData == state.existingSecurityPlan) {
            clearEditingPlanData()
            return
        }

        state = state.copy(showCancelPlanSetupDialog = true)
    }

    private fun saveCurrentPlan() {
        val securityPlanData = SecurityPlanData(
            guardians = state.guardians,
            threshold = state.threshold
        )

        ownerRepository.setEditingSecurityPlan(true)
        ownerRepository.saveSecurityPlanData(
            securityPlanData
        )
    }
}