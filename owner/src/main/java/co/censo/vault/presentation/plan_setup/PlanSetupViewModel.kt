package co.censo.vault.presentation.plan_setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart() {
        /*if (ownerRepository.isUserEditingSecurityPlan()) {
            val securityPlanData = ownerRepository.retrieveSecurityPlan()

            securityPlanData?.let {
                state = state.copy(
                    currentScreen = SetupSecurityPlanScreen.Review,
                    threshold = it.threshold,
                    guardians = it.guardians
                )
            }
        }*/
    }

    /*suspend fun onPolicySetupCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        /*state = state.copy(createPolicySetupResponse = Resource.Loading())

        return viewModelScope.async {
            val createPolicySetupResponse = ownerRepository.createPolicySetup(
                state.threshold,
                state.guardians.map {
                    Guardian.SetupGuardian.ExternalApprover(
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

                // notify lock screen about facetec enrollment
                // this will activate lock tracking
                ownerStateFlow.tryEmit(createPolicySetupResponse.map { it.ownerState })
            } else if (createPolicySetupResponse is Resource.Error) {
                state = state.copy(
                    createPolicySetupResponse = createPolicySetupResponse,
                    currentScreen = SetupSecurityPlanScreen.SecureYourPlan
                )
            }

            createPolicySetupResponse.map { it.scanResultBlob }
        }.await()*/

        return Resource.Success(BiometryScanResultBlob(""))
    }
*/
    fun onBackActionClick() {
        // FIXME
/*
        state = when (state.planSetupUIState) {
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
        }*/
    }

   /* fun onMainActionClick() {
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

    fun showEditOrDeleteDialog(guardian: Guardian.SetupGuardian.ExternalApprover) {
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
                guardians[index] = Guardian.SetupGuardian.ExternalApprover(
                    label = state.addedApproverNickname,
                    participantId = state.editingGuardian!!.participantId,
                    deviceEncryptedTotpSecret = state.editingGuardian!!.deviceEncryptedTotpSecret
                )
            }
        } else {
            val secret = TotpGenerator.generateSecret()
            val encryptedSecret = keyRepository.encryptWithDeviceKey(secret.toByteArray())

            guardians.add(
                Guardian.SetupGuardian.ExternalApprover(
                    label = state.addedApproverNickname,
                    participantId = ParticipantId(
                        generatePartitionId()
                    ),
                    deviceEncryptedTotpSecret = encryptedSecret.base64Encoded()
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
    }*/

    fun resetNavToActivateApprovers() {
        state = state.copy(navigateToActivateApprovers = false)
    }

   /* fun retryFacetec() {
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

        /*if (editedSecurityPlanData == state.existingSecurityPlan) {
            clearEditingPlanData()
            return
        }*/

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
    */

    fun reset() {
        state = PlanSetupState()
    }

    fun onInvitePrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverNickname
        )
    }

    fun primaryApproverNicknameChanged(nickname: String) {
        state = state.copy(
            primaryApproverNickname = nickname
        )
    }

    fun backupApproverNicknameChanged(nickname: String) {
        state = state.copy(
            backupApproverNickname = nickname
        )
    }

    fun onContinueWithPrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverGettingLive
        )
    }

    fun onContinueWithBackupApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.BackupApproverGettingLive
        )
    }

    fun onInviteBackupApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.AddBackupApprover
        )
    }

    fun saveAndFinish() {

    }

    fun onPrimaryApproverVerification() {


        /*// FIXME activate resource

        val secret = TotpGenerator.generateSecret()
        val encryptedSecret = keyRepository.encryptWithDeviceKey(secret.toByteArray())

        val primaryApprover = Guardian.SetupGuardian.ExternalApprover(
            label = state.primaryApproverNickname,
            participantId = ParticipantId(
                generatePartitionId()
            ),
            deviceEncryptedTotpSecret = encryptedSecret.base64Encoded()
        )

        state = state.copy(
            primaryApprover = primaryApprover
        )

        // FIXME submit policy setup*/

        /*ownerRepository.createPolicySetup(
            threshold = 2,
            guardians =
        )*/

        // FIXME initialize code, timer. Send request to the server
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverActivation
        )
    }

    fun onBackupApproverVerification() {
        state = state.copy(
            // FIXME go first to the backup approver activation
            planSetupUIState = PlanSetupUIState.Completed
        )
    }

}