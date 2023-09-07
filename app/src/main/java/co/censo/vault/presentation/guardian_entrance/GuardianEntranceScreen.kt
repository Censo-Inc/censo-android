package co.censo.vault.presentation.guardian_entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
        onDispose {  }
    }

    Text(text = "Guardian Entrance")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val verificationCode: MutableState<String> = remember { mutableStateOf("") }

        when (state.guardianStatus) {
            GuardianStatus.ENTER_VERIFICATION_CODE -> {
                Text(text = "Enter verification code")

                TextField(value = verificationCode.value, onValueChange = {
                    verificationCode.value = it
                })
            }

            GuardianStatus.REGISTER_GUARDIAN -> {
                Text(text = "Register guardian")
            }
        }
    }
}