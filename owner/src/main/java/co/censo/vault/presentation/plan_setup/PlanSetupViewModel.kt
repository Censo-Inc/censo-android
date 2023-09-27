package co.censo.vault.presentation.plan_setup

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Guardian
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PlanSetupViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

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
                //todo: show facetec
            }
        }
    }

    fun showAddGuardianDialog() {
        state = state.copy(showAddGuardianDialog = true)
    }

    fun updateSliderPosition(updatedPosition: Float) {
        state = state.copy(
            thresholdSliderPosition =
            updatedPosition.roundToInt().toFloat()
        )
    }

    fun onMoreInfoClicked() {

    }

    fun showEditOrDeleteDialog(guardian: Guardian) {
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
                editingGuardian = null,
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
                threshold = state.thresholdSliderPosition.toInt()
            )

            state = state.copy(
                editingGuardian = null,
                guardians = guardians,
                showEditOrDeleteDialog = false,
                thresholdSliderPosition = threshold.toFloat()
            )
        }
    }

    fun submitNewApprover() {
        val guardians = state.guardians.toMutableList()

        if (state.editingGuardian != null) {
            val index = guardians.indexOf(state.editingGuardian!!)

            if (index != -1) {
                guardians[index] = Guardian.SetupGuardian(
                    label = state.addedApproverNickname,
                    participantId = state.editingGuardian!!.participantId
                )
            }
        } else {
            guardians.add(
                Guardian.SetupGuardian(
                    label = state.addedApproverNickname,
                    participantId = ParticipantId(
                        generatePartitionId().toHexString()
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
    }

    fun updateAddedApproverNickname(updatedNickname: String) {
        state = state.copy(addedApproverNickname = updatedNickname)
    }

    fun dismissDialog() {
        state = state.copy(
            addedApproverNickname = "",
            editingGuardian = null,
            showAddGuardianDialog = false,
            showEditOrDeleteDialog = false
        )
    }

    private fun recalculateThreshold(
        previousGuardianSize: Int,
        currentGuardianSize: Int,
        threshold: Int
    ): Int {
        val difference = previousGuardianSize - threshold
        val newThreshold = currentGuardianSize - difference

        return if (newThreshold >= 1) newThreshold else 1
    }
}