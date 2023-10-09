package co.censo.vault.presentation.recovery

import FullScreenButton
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Recovery
import co.censo.shared.presentation.Colors
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.components.recovery.AnotherDeviceRecoveryScreen
import co.censo.vault.presentation.components.recovery.RecoveryApprovalRow
import co.censo.vault.presentation.components.recovery.RecoveryApprovalsCollected
import co.censo.vault.presentation.components.recovery.RecoveryExpirationCountDown

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RecoveryScreen(
    navController: NavController,
    viewModel: RecoveryScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
            }
            viewModel.reset()
        }

        if (state.initiateNewRecovery) {
            viewModel.initiateRecovery()
        }
    }

    when {
        state.loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Colors.PrimaryBlue)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.White
                )
            }
        }

        state.asyncError -> {
            when {
                state.ownerStateResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.ownerStateResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.reloadOwnerState() }
                }

                state.initiateRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.initiateRecoveryResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.initiateRecovery() }
                }

                state.cancelRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.cancelRecoveryResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.cancelRecovery() }
                }
            }
        }

        else -> {

            when (val recovery = state.recovery) {
                null -> {
                    // recovery is about to be requested
                }

                is Recovery.AnotherDevice -> {
                    AnotherDeviceRecoveryScreen()
                }

                is Recovery.ThisDevice -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(color = Colors.PrimaryBlue)
                            .padding(all = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Recovery Initiated",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.W700,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RecoveryExpirationCountDown(expiresAt = recovery.expiresAt) {
                            viewModel.cancelRecovery()
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FullScreenButton(
                            modifier = Modifier.padding(horizontal = 72.dp),
                            color = Colors.PrimaryBlue,
                            borderColor = Color.White,
                            border = true,
                            contentPadding = PaddingValues(vertical = 0.dp),
                            onClick = viewModel::cancelRecovery,
                        ) {
                            Text(
                                text = "Cancel Recovery",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W400
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        RecoveryApprovalsCollected(
                            collected = state.approvalsCollected,
                            required = state.approvalsRequired
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "required approvals reached to complete recovery",
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Tap the ",
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = stringResource(R.string.previous_icon_content_desc),
                                tint = Color.White
                            )
                            Text(
                                text = " icon next to each of your approvers to send them the recovery link",
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(all = 16.dp)
                        ) {
                            val approvals = recovery.approvals

                            items(approvals.size) { index ->
                                val approval = approvals[index]
                                RecoveryApprovalRow(
                                    guardian = state.guardians.first { it.participantId == approval.participantId },
                                    approval = approval
                                )

                                if (index != approvals.size - 1)
                                    Divider(
                                        color = Color.White.copy(alpha = 0.1f),
                                        thickness = 1.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}


