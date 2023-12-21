package co.censo.censo.presentation.owner_key_validation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.presentation.owner_key_validation.components.InvalidOwnerKeyUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler

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
            state.navigationResource.data?.let {
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
        when (state.ownerKeyUIState) {
            OwnerKeyValidationState.OwnerKeyValidationUIState.FileNotFound -> {
                InvalidOwnerKeyUI(onInitiateRecovery = viewModel::navigateToKeyRecovery)
            }

            else -> { }
        }
    }
}