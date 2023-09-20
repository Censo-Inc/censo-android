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

    Text(text = stringResource(R.string.guardian_entrance))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.apiError) {
            Text(text = "Error completing action")
        }

        when (state.guardianUIState) {
            GuardianUIState.UNINITIALIZED -> {
                Text("Loading...")
            }

            GuardianUIState.USER_LOADED -> {
                Text("Welcome...")
            }

            GuardianUIState.HAS_INVITE_CODE -> {
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

            GuardianUIState.ACCEPTED_INVITE -> {
                Text(text = "Accepted Guardianship!", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Now you need to verify your identity. Please enter the code the phrase owner sent you...")
                Button(onClick = { viewModel.submitVerificationCode() }) {
                    Text(text = "Submit code")
                }
            }

            GuardianUIState.DECLINED_INVITE -> {
                Text(text = "Declined Guardianship", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "I said good day!")
            }

            GuardianUIState.VERIFIED -> {
                Text(text = "Guardian Sent Verification...", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Owner needs to verify the code...")
            }
        }
    }
}