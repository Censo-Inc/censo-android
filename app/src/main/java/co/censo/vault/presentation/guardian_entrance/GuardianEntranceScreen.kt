package co.censo.vault.presentation.guardian_entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.vault.R
import co.censo.vault.data.Resource
import co.censo.vault.util.BiometricUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianEntranceScreen(
    args: GuardianEntranceArgs,
    viewModel: GuardianEntranceViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.bioPromptTrigger is Resource.Success) {
            val promptInfo = BiometricUtil.createPromptInfo(context)

            val bioPrompt = BiometricUtil.createBioPrompt(
                fragmentActivity = context,
                onSuccess = {
                    viewModel.onBiometryApproved()
                },
                onFail = {
                    BiometricUtil.handleBioPromptOnFail(context = context, errorCode = it) {
                        viewModel.onBiometryFailed()
                    }
                }
            )

            bioPrompt.authenticate(promptInfo)
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(args)
        onDispose { }
    }

    Text(text = stringResource(R.string.guardian_entrance))

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.bioPromptTrigger is Resource.Error) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.successful_biometry_authentication_is_required_to_use_this_app),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = {
                state.bioPromptTrigger.data?.let {
                    viewModel.resetBioPromptTrigger()
                    viewModel.triggerBiometryPrompt(it)
                }
            }) {
                Text(text = stringResource(id = R.string.retry))
            }
        } else {
            when (state.guardianStatus) {
                GuardianStatus.WAITING_FOR_CODE -> {
                    if (state.acceptGuardianshipResource is Resource.Error) {
                        Text(text = stringResource(R.string.accept_guardianship_error))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = state.acceptGuardianshipResource.getErrorMessage(context),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerBiometryPrompt(
                                    biometryAuthReason = BiometryAuthReason.SUBMIT_VERIFICATION_CODE
                                )
                            }) {
                            Text(stringResource(id = R.string.retry))
                        }
                    } else if (state.declineGuardianshipResource is Resource.Error) {
                        Text(text = stringResource(R.string.decline_guardianship_error))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = state.declineGuardianshipResource.getErrorMessage(context),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.declineGuardianship()
                            }) {
                            Text(stringResource(id = R.string.retry))
                        }
                    } else {
                        if (state.declineGuardianshipResource is Resource.Loading) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = stringResource(R.string.you_have_been_invited_to_vault_as_a_guardian_do_you_accept_guardianship),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.enter_verification_code))
                            Spacer(modifier = Modifier.height(12.dp))

                            TextField(value = state.verificationCode, onValueChange = {
                                viewModel.updateVerificationCode(it)
                            }, singleLine = true,)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                enabled = state.isVerificationCodeValid,
                                onClick = {
                                    viewModel.triggerBiometryPrompt(
                                        biometryAuthReason = BiometryAuthReason.SUBMIT_VERIFICATION_CODE
                                    )
                                }) {
                                if (state.acceptGuardianshipResource is Resource.Loading) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(stringResource(id = R.string.submit))
                                }
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            Text(text = stringResource(R.string.to_decline_guardianship_tap_the_button_below))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.declineGuardianship()
                                }) {
                                Text(stringResource(R.string.decline))
                            }
                        }
                    }
                }

                GuardianStatus.REGISTER_GUARDIAN -> {
                    if (state.registerGuardianResource is Resource.Error) {
                        Text(text = stringResource(R.string.register_guardian_error))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = state.registerGuardianResource.getErrorMessage(context),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerBiometryPrompt(
                                    biometryAuthReason = BiometryAuthReason.REGISTER_GUARDIAN
                                )
                            }) {
                            Text(stringResource(id = R.string.retry))
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }

                GuardianStatus.WAITING_FOR_SHARD -> {
                    Text(text = stringResource(R.string.awaiting_confirmation_of_verification_code))
                }

                GuardianStatus.DATA_MISSING -> {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.missing_guardian_arg_data),
                        textAlign = TextAlign.Center
                    )
                }

                GuardianStatus.DECLINED -> {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.declined_guardianship),
                        textAlign = TextAlign.Center
                    )
                }

                GuardianStatus.SHARD_RECEIVED -> {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.received_shard_from_owner),
                        textAlign = TextAlign.Center
                    )
                }
                GuardianStatus.COMPLETE -> {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.guardian_setup_complete),
                        textAlign = TextAlign.Center
                    )
                }

                GuardianStatus.UNINITIALIZED -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}