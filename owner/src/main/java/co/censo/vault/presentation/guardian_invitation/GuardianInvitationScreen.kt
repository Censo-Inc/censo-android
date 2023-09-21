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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.SharedScreen
import co.censo.shared.SharedScreen.Companion.GUARDIAN_URI
import co.censo.vault.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import co.censo.vault.presentation.facetec_auth.FacetecAuth

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
            .background(color = Color.White)
            .verticalScroll(rememberScrollState()),
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
                if (state.userResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.userResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetUserResponse,
                    ) {
                        viewModel.retrieveUserState()
                    }
                } else if (state.createPolicyResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.createPolicyResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetCreatePolicyResource,
                    ) {
//                        viewModel.createPolicy()
                    }
                } else if (state.createGuardianResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.createGuardianResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetCreateGuardianResource
                    ) {
                        viewModel.addGuardian()
                    }
                } else if (state.inviteGuardianResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.inviteGuardianResponse.getErrorMessage(context),
                        dismissAction = viewModel::resetInviteResource,
                    ) { viewModel.resetInviteResource() }
                }
            }

            else -> {
                when (state.guardianInviteStatus) {
                    GuardianInvitationStatus.ENUMERATE_GUARDIANS -> {
                        if (state.createdGuardians.isEmpty()) {
                            Text(
                                text = stringResource(R.string.guardians_empty_message),
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            val clipboardManager = LocalClipboardManager.current

                            Column(modifier = Modifier.fillMaxWidth()) {

                                for (guardian in state.createdGuardians) {
                                    if (guardian is Guardian.ProspectGuardian) {

                                        val guardianStatus = guardian.status

                                        val deeplink =
                                            "$GUARDIAN_URI${guardian.invitationId?.value}"

                                        val copyDeeplink = {
                                            clipboardManager.setText(
                                                AnnotatedString(
                                                    deeplink
                                                )
                                            )
                                        }

                                        val inviteGuardian = { viewModel.inviteGuardian(guardian.participantId) }

                                        when (guardianStatus) {
                                            is GuardianStatus.Invited -> {
                                                InvitedGuardianUI(
                                                    guardianLabel = guardian.label,
                                                    deepLink = deeplink,
                                                    copyDeeplink = copyDeeplink
                                                )
                                            }
                                            is GuardianStatus.Initial -> {
                                                InitialGuardianUI(
                                                    guardianLabel = guardian.label,
                                                    inviteGuardian = inviteGuardian,
                                                    deepLink = deeplink,
                                                    copyDeeplink = copyDeeplink
                                                )
                                            }
                                            is GuardianStatus.Accepted -> {
                                                AcceptedGuardianUI(guardianLabel = guardian.label)
                                            }

                                            is GuardianStatus.VerificationSubmitted -> {
                                                SubmittedVerificationGuardianUI(
                                                    guardian.label,
                                                ) {
                                                    viewModel.verifyGuardian(
                                                        guardian, guardianStatus
                                                    )
                                                }
                                            }

                                            is GuardianStatus.Confirmed -> {
                                                ConfirmedGuardianUI(
                                                    guardianLabel = guardian.label
                                                )
                                            }

                                            else -> {
                                                InitialGuardianUI(
                                                    guardianLabel = guardian.label,
                                                    inviteGuardian = inviteGuardian,
                                                    deepLink = deeplink,
                                                    copyDeeplink = copyDeeplink
                                                )
                                            }
                                        }
                                    } else if (guardian is Guardian.TrustedGuardian) {
                                        Text(
                                            text = "Trusted ${guardian.label}",
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(36.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TextButton(
                                onClick = { viewModel.addGuardian() },
                                enabled = state.createGuardianResponse !is Resource.Loading
                            ) {
                                if (state.createGuardianResponse is Resource.Loading) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        text = stringResource(R.string.add_guardian),
                                        color = Color.Black
                                    )
                                }
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
                            onClick = { viewModel.onUserCreatedGuardianSet() },
                            enabled = state.canContinueOnboarding
                        ) {
                            Text(
                                text = stringResource(R.string.continue_onboarding),
                                color = Color.Black
                            )
                        }
                    }

                    GuardianInvitationStatus.INVITE_GUARDIANS -> {
                        val clipboardManager = LocalClipboardManager.current

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
                            items(state.createdGuardians.size) { index ->
                                val guardian = state.createdGuardians[index]
                                val deeplink = if (guardian is Guardian.ProspectGuardian) "$GUARDIAN_URI${guardian.invitationId?.value}" else "Guardian Already Added"

                                InitialGuardianUI(
                                    guardianLabel = state.createdGuardians[index].label,
                                    inviteGuardian = { viewModel.inviteGuardian(guardian.participantId) },
                                    deepLink = deeplink,
                                    copyDeeplink = {
                                        clipboardManager.setText(
                                            AnnotatedString(
                                                deeplink
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        TextButton(
                            onClick = {
                                projectLog(message = "Continue to policy creation")
                                viewModel.enrollBiometry()
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.create_protection_plan),
                                color = Color.Black
                            )
                        }
                    }

                    GuardianInvitationStatus.CREATE_POLICY -> {
                        FacetecAuth(
                            onFaceScanReady = viewModel::onFaceScanReady
                        )
                    }

                    GuardianInvitationStatus.READY -> {
                        Text(
                            text = "Guardian Set Created Successfully",
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Spacer(Modifier.height(24.dp))

                        TextButton(
                            onClick = {
                                navController.navigate(SharedScreen.HomeRoute.route)
                            },
                        ) {
                            Text(
                                text = "Ok",
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmedGuardianUI(guardianLabel: String) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(color = Color(0xFF4059AD))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 36.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$guardianLabel Confirmed",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubmittedVerificationGuardianUI(
    guardianLabel: String,
    verifyGuardian: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(color = Color(0xFF4059AD))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 36.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$guardianLabel Code Submitted",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = verifyGuardian) {
                Text("Verify Guardian")
            }
        }
    }
}

@Composable
fun AcceptedGuardianUI(
    guardianLabel: String,
) {
    val context = LocalContext.current as FragmentActivity

    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .background(color = Color(0xFF4059AD))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 36.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(text = "$guardianLabel Accepted", color = Color.White, fontSize = 24.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Text("No more actions required.", color = Color.White)
        }
    }
}

@Composable
fun InvitedGuardianUI(
    guardianLabel: String,
    deepLink: String,
    copyDeeplink: () -> Unit,
    ) {
    val context = LocalContext.current as FragmentActivity

    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .background(color = Color(0xFF4059AD))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 36.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(text = "$guardianLabel Invited!", color = Color.White, fontSize = 24.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                IconButton(onClick = copyDeeplink) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy icon",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))


                IconButton(onClick = {
                    shareDeeplink(
                        deeplink = deepLink,
                        context
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share icon",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InitialGuardianUI(
    guardianLabel: String,
    deepLink: String,
    copyDeeplink: () -> Unit,
    inviteGuardian: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity

    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 36.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(text = guardianLabel, color = Color.White, fontSize = 24.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = inviteGuardian) {
                Text("Invite", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                IconButton(onClick = copyDeeplink) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy icon",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))


                IconButton(onClick = {
                    shareDeeplink(
                        deeplink = deepLink,
                        context
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share icon",
                        tint = Color.White
                    )
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