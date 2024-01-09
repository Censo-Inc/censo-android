package co.censo.approver.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.presentation.components.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverSettingsScreen(
    navController: NavController,
    viewModel: ApproverSettingsViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let { navigationData ->
                navController.navigate(navigationData.route)
            }
            viewModel.resetNavigationResource()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(id = R.string.close),
                        )
                    }
                },
                title = { }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.loading -> {
                    LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
                }

                state.asyncError -> {
                    if (state.deleteUserResource is Resource.Error) {
                        DisplayError(
                            errorMessage = state.deleteUserResource.getErrorMessage(context),
                            dismissAction = viewModel::resetDeleteUserResource,
                            retryAction = null
                        )
                    } else if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.userResponse.getErrorMessage(context),
                            retryAction = { viewModel.retrieveApproverState() },
                            dismissAction = null
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        Text(
                            text = stringResource(R.string.settings),
                            fontSize = 38.sp,
                            color = SharedColors.MainColorText
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.White)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val activeApproversCount = (state.userResponse as? Resource.Success)?.data?.activeApproversCount ?: 0
                            if (activeApproversCount > 1) {
                                Divider()
                                SettingsItem(
                                    title = stringResource(R.string.view_owners_settings_title),
                                    buttonText = stringResource(R.string.view_owners_settings_button),
                                    description = stringResource(R.string.view_owners_settings_description),
                                    onSelected = viewModel::navToOwnersListScreen
                                )
                            }

                            Divider()
                            SettingsItem(
                                title = stringResource(R.string.delete_data_settings_title),
                                buttonText = stringResource(R.string.delete_data_settings_button),
                                description = stringResource(R.string.delete_data_settings_description),
                                onSelected = viewModel::setShowDeleteUserConfirmDialog
                            )
                        }
                    }

                    if (state.showDeleteUserConfirmDialog) {
                        ConfirmationDialog(
                            title = stringResource(R.string.deactivate_delete),
                            message = stringResource(R.string.deactivate_delete_message),
                            onCancel = viewModel::resetShowDeleteUserConfirmDialog,
                            onDelete = viewModel::deleteUser,
                        )
                    }
                }
            }
        }
    }
}