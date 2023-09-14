package co.censo.vault.presentation.guardian_invitation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianStatus.Initial.*
import co.censo.shared.data.model.PolicyGuardian
import co.censo.vault.presentation.owner_entrance.DisplayError
import co.censo.vault.util.vaultLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianInvitationScreen(
    navController: NavController, viewModel: GuardianInvitationViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.White)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center),
                        strokeWidth = 8.dp,
                        color = Color.Red
                    )
                }
            }

            state.asyncError -> {
                if (state.incorrectPinCode) {
                    DisplayError(
                        errorMessage = "Pin code did not match. Please try again.",
                        dismissAction = viewModel::resetConfirmShardReceipt,
                    ) { viewModel.resetConfirmShardReceipt() }
                } else if (state.userResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.userResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetUserResponse,
                    ) { viewModel.retrieveUserState(GuardianInvitationViewModel.createPolicyGetUserApiResponse) }
                } else if (state.createPolicyResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.createPolicyResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetCreatePolicyResource,
                    ) { viewModel.createPolicy() }
                } else if (state.inviteGuardianResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.inviteGuardianResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetInviteResource,
                    ) { viewModel.resetInviteResource() }
                }
            }

            else -> {
                when (state.guardianInviteStatus) {
                    GuardianInvitationStatus.CREATE_POLICY -> {
                        if (state.potentialGuardians.isEmpty()) {
                            Text(
                                text = stringResource(R.string.guardians_empty_message),
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            LazyColumn {
                                items(state.potentialGuardians.size) { index ->
                                    Text(
                                        text = state.potentialGuardians[index],
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TextButton(onClick = { viewModel.addGuardian() }) {
                                Text(
                                    text = stringResource(R.string.add_guardian),
                                    color = Color.Black
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = stringResource(R.string.threshold), color = Color.Black)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    IconButton(onClick = {
                                        viewModel.updateThreshold(state.threshold - 1)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = stringResource(R.string.remove_content_desc),
                                            tint = Color.Black
                                        )
                                    }

                                    Text(text = state.threshold.toString(), color = Color.Black)

                                    IconButton(onClick = {
                                        viewModel.updateThreshold(state.threshold + 1)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_content_desc),
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = { viewModel.userSubmitGuardianSet() },
                            enabled = state.canContinueOnboarding
                        ) {
                            Text(
                                text = stringResource(R.string.continue_onboarding),
                                color = Color.Black
                            )
                        }
                    }

                    GuardianInvitationStatus.POLICY_SETUP -> {
                        Spacer(modifier = Modifier.height(56.dp))

                        Text(
                            text = stringResource(R.string.invite_guardians),
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Spacer(Modifier.height(24.dp))

                        LazyColumn(
                            modifier = Modifier
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(state.prospectGuardians.size) { index ->
                                val guardian = state.prospectGuardians[index]

                                InvitedGuardian(
                                    guardian = state.prospectGuardians[index],
                                    inviteGuardian = { viewModel.inviteGuardian(guardian) },
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.scan_guardian_qr_code),
                            color = Color.Black
                        )

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                vaultLog(message = "Scanning QR code")
                                //TODO: Implement QR code scanning
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.scan_qr_code),
                                color = Color.Black
                            )
                        }
                    }

                    GuardianInvitationStatus.READY -> {
                        Text(
                            text = "Guardian Set Created Successfully",
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptedGuardian(
    guardian: PolicyGuardian.ProspectGuardian,
    checkAcceptedGuardian: () -> Unit
) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(
            modifier = Modifier
                .background(color = Color(0xFF4059AD))
                .padding(vertical = 12.dp, horizontal = 36.dp)
        ) {
            Text(
                text = "Guardian Accepted: ${guardian.label}",
                color = Color.White,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = checkAcceptedGuardian) {
                Text(text = "Verify Guardian Code")
            }
        }
    }
}

@Composable
fun InvitedGuardian(
    guardian: PolicyGuardian.ProspectGuardian,
    inviteGuardian: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity

    when (guardian.status) {
        else -> {
            Card(modifier = Modifier.padding(8.dp)) {
                Column(modifier = Modifier
                    .background(color = Color(0xFF4059AD))
                    .padding(vertical = 12.dp, horizontal = 36.dp)) {

                    Text(text = guardian.label, color = Color.White, fontSize = 24.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    IconButton(onClick = inviteGuardian) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "invite guardian",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}


fun shareDeeplink(deeplink: String, context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, deeplink)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}