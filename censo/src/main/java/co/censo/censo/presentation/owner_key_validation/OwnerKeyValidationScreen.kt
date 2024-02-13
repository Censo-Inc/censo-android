package co.censo.censo.presentation.owner_key_validation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.components.DeleteUserConfirmationUI
import co.censo.censo.presentation.owner_key_validation.components.InvalidOwnerKeyUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError

@Composable
fun ValidateApproverKeyScreen(
    navController: NavController,
    viewModel: OwnerKeyValidationViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> viewModel.onCreate()
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let {
                navController.navigate(it)
            }
            viewModel.resetAfterNavigation()
        }
    }

    Box(modifier = Modifier
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = {}
        )
    ) {
        when (state.apiResource) {
            is Resource.Error -> {
                DisplayError(
                    errorMessage = "Error occurred while trying to check for saved cloud data. Please try again.",
                    dismissAction = null,
                    retryAction = {
                        viewModel.resetErrorState()
                        viewModel.validateApproverKey(state.idToCheckForCloudSavedKey)
                    })
            }

            else -> {
                when (state.ownerKeyUIState) {
                    OwnerKeyValidationState.OwnerKeyValidationUIState.FileNotFound -> {
                        InvalidOwnerKeyUI(
                            onInitiateRecovery = viewModel::navigateToKeyRecovery,
                            onDelete = viewModel::triggerDeleteUserDialog
                        )

                        if (state.triggerDeleteUserDialog is Resource.Success) {
                            DeleteUserConfirmationUI(
                                title = stringResource(id = R.string.cancel_key_recovery),
                                seedCount = state.ownerState?.vault?.seedPhrases?.size ?: 0,
                                onCancel = viewModel::onCancelDeleteUserDialog,
                                onDelete = viewModel::deleteUser,
                            )
                        }
                    }

                    else -> { }
                }
            }
        }
    }
}