package co.censo.guardian.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import co.censo.guardian.R
import co.censo.guardian.presentation.components.ApproverCodeVerification
import co.censo.shared.data.Resource

@Composable
fun GuardianHomeScreen(
    navController: NavController,
    viewModel: GuardianHomeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    val (parentColumnHorizontalPadding,
        parentColumnVerticalPadding) =
        if (state.guardianUIState == GuardianUIState.WAITING_FOR_CODE) {
            Pair(0.dp, 0.dp)
        } else {
            Pair(16.dp, 12.dp)
        }

    if (state.guardianUIState != GuardianUIState.WAITING_FOR_CODE) {
        Text(text = stringResource(R.string.guardian_entrance))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = parentColumnHorizontalPadding, vertical = parentColumnVerticalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.apiError && state.guardianUIState != GuardianUIState.WAITING_FOR_CODE) {
            Text(text = "Error completing action")
        }

        if (state.guardianUIState != GuardianUIState.WAITING_FOR_CODE) {
            Spacer(modifier = Modifier.height(48.dp))
        }

        when (val guardianUIState = state.guardianUIState) {
            GuardianUIState.UNINITIALIZED -> {
                Text(
                    "Loading...",
                    textAlign = TextAlign.Center
                )
            }

            GuardianUIState.WAITING_FOR_CONFIRMATION -> {
                Text(
                    "Code sent to owner, waiting for them to approve. Re-send below...",
                    textAlign = TextAlign.Center
                )
                ApproverCodeVerification(
                    value = state.verificationCode,
                    onValueChanged = viewModel::updateVerificationCode,
                    errorResource = if (state.submitVerificationResource is Resource.Error) state.submitVerificationResource
                    else null,
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
                        isLoading = state.submitVerificationResource is Resource.Loading,
                    )
                }
            }

            GuardianUIState.INVITE_READY -> {
                Text(
                    text = "Looks like you have been invited to be a guardian!", fontSize = 18.sp,
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
                Text(
                    "Fully onboarded!",
                    textAlign = TextAlign.Center
                )
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