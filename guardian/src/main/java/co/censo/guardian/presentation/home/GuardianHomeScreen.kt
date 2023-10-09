package co.censo.guardian.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.guardian.R
import co.censo.guardian.presentation.GuardianColors
import co.censo.guardian.presentation.components.ApproverCodeVerification
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianHomeScreen(
    navController: NavController,
    viewModel: GuardianHomeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START
            -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GuardianColors.PrimaryColor),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.become_an_approver),
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val guardianUIState = state.guardianUIState) {
                    GuardianUIState.UNINITIALIZED -> {
                        Spacer(modifier = Modifier.height(56.dp))
                        CircularProgressIndicator(
                            color = GuardianColors.PrimaryColor,
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 5.dp,
                        )
                    }

                    GuardianUIState.WAITING_FOR_CONFIRMATION -> {
                        ApproverCodeVerification(
                            value = state.verificationCode,
                            onValueChanged = viewModel::updateVerificationCode,
                            errorResource = if (state.submitVerificationResource is Resource.Error) state.submitVerificationResource
                            else null,
                            label = stringResource(R.string.code_sent_to_owner_waiting_for_them_to_approve),
                            isLoading = state.submitVerificationResource is Resource.Loading,
                        )
                    }

                    GuardianUIState.CODE_REJECTED -> {
                        ApproverCodeVerification(
                            value = state.verificationCode,
                            onValueChanged = viewModel::updateVerificationCode,
                            errorResource = if (state.submitVerificationResource is Resource.Error) state.submitVerificationResource
                            else null,
                            label = stringResource(R.string.code_not_approved),
                            isLoading = state.submitVerificationResource is Resource.Loading,
                        )
                    }

                    GuardianUIState.MISSING_INVITE_CODE -> {
                        Text(
                            text = "No invite code detected. Please click the invite link your Owner sent you.",
                            textAlign = TextAlign.Center
                        )
                    }

                    GuardianUIState.WAITING_FOR_CODE, GuardianUIState.NEED_SAVE_KEY -> {
                        if (guardianUIState == GuardianUIState.NEED_SAVE_KEY) {
                            Text(
                                text = "You have accepted the guardianship!", fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            Text(
                                text = "We need you to create and store your Guardian key. This will be used to complete recovery if the owner loses their phrase.",
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.createGuardianKey() }) {
                                Text("Create Guardian Key")
                            }
                        } else {
                            ApproverCodeVerification(
                                value = state.verificationCode,
                                onValueChanged = viewModel::updateVerificationCode,
                                errorResource = if (state.submitVerificationResource is Resource.Error) state.submitVerificationResource
                                else null,
                                label = stringResource(R.string.approver_verification_code_text),
                                isLoading = state.submitVerificationResource is Resource.Loading,
                            )
                        }
                    }

                    GuardianUIState.INVITE_READY -> {
                        Text(
                            text = "Looks like you have been invited to be a guardian!",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))



                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = {
                                viewModel.declineGuardianship()
                            }) {
                                Text(text = "Decline", color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(36.dp))

                            Button(onClick = {
                                viewModel.acceptGuardianship()
                            }) {
                                Text(text = "Accept", color = Color.White)
                            }
                        }
                    }

                    GuardianUIState.COMPLETE -> {
                        Spacer(modifier = Modifier.height(30.dp))
                        //Text
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "Fully onboarded!",
                            color = GuardianColors.PrimaryColor,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(30.dp))
                    }

                    GuardianUIState.DECLINED_INVITE -> {
                        Text(
                            text = "Declined Guardianship",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "I said good day!",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }
    )
}