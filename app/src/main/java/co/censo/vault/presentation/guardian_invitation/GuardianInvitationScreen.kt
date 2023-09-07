package co.censo.vault.presentation.guardian_invitation

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.data.Resource
import co.censo.vault.util.BiometricUtil
import co.censo.vault.util.vaultLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianInvitationScreen(
    navController: NavController, viewModel: GuardianInvitationViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {

        if (state.bioPromptTrigger is Resource.Success) {

            val promptInfo = BiometricUtil.createPromptInfo(context = context)

            val bioPrompt = BiometricUtil.createBioPrompt(
                fragmentActivity = context,
                onSuccess = {
                    viewModel.onBiometryApproved()
                },
                onFail = {
                    BiometricUtil.handleBioPromptOnFail(
                        context = context,
                        errorCode = it
                    ) {
                        viewModel.onBiometryFailed()
                    }
                }
            )

            bioPrompt.authenticate(promptInfo)
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (state.guardianInviteStatus) {
            GuardianInvitationStatus.ADD_GUARDIANS -> {
                if (state.guardians.isEmpty()) {
                    Text(
                        text = stringResource(R.string.guardians_empty_message),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    LazyColumn {
                        items(state.guardians.size) { index ->
                            Text(text = state.guardians[index].name)
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
                        Text(text = stringResource(R.string.add_guardian))
                    }

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = stringResource(R.string.threshold))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(onClick = {
                                viewModel.updateThreshold(state.threshold - 1)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.remove_content_desc)
                                )
                            }

                            Text(text = state.threshold.toString())

                            IconButton(onClick = {
                                viewModel.updateThreshold(state.threshold + 1)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_content_desc)
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
                    Text(text = stringResource(R.string.continue_onboarding))
                }
            }
            GuardianInvitationStatus.INVITE_GUARDIANS -> {
                val clipboardManager = LocalClipboardManager.current

                Text(text = stringResource(R.string.share_guardian_deeplinks), textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(state.guardianDeepLinks.size) { index ->

                        Card(modifier = Modifier.padding(8.dp), onClick = {
                            clipboardManager.setText(AnnotatedString(state.guardianDeepLinks[index]))
                        }) {
                            Row {
                                Text(
                                    modifier = Modifier.padding(12.dp),
                                    text = state.guardians[index].name
                                )

                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(state.guardianDeepLinks[index]))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy icon"
                                    )
                                }

                                IconButton(onClick = {
                                    shareDeeplink(deeplink = state.guardianDeepLinks[index], context)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share icon"
                                    )
                                }
                            }
                        }
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