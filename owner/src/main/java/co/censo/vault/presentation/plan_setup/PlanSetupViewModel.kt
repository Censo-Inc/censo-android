package co.censo.vault.presentation.plan_setup

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * 1. If we are actively editing the plan then take user to review screen
 */

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart() {
    }

    fun onBackActionClick() {
        if (state.currentScreen == SetupSecurityPlanScreen.RequiredApprovals) {
            state = state.copy(
                currentScreen = SetupSecurityPlanScreen.AddApprovers
            )
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
                //todo: Send user to facetec approval
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

            projectLog(message = "Index of edited guardian is: $index")

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

        projectLog(message = "Difference: $difference")

        val newThreshold = currentGuardianSize - difference

        projectLog(message = "New threshold: $newThreshold")

        return if (newThreshold >= 1) newThreshold else 1
    }
}