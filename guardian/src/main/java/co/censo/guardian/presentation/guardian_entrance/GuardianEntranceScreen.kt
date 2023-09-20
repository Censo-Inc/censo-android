package co.censo.guardian.presentation.guardian_entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.guardian.R

@Composable
fun GuardianHome(
    navController: NavController,
    viewModel: GuardianEntranceViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    Text(text = stringResource(R.string.guardian_entrance))

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (state.guardianStatus) {
            GuardianStatus.UNINITIALIZED -> {
                CircularProgressIndicator()
            }

            GuardianStatus.LOGIN -> {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.authenticate_with_onetap),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                TextButton(onClick = { viewModel.registerGuardian() }) {
                    Text(text = stringResource(id = R.string.login))
                }
            }
            GuardianStatus.REGISTER -> {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.registering_guardian),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))
                
                TextButton(onClick = { viewModel.registerGuardian() }) {
                    Text(text = stringResource(id = R.string.register))
                }
            }

            GuardianStatus.DISPLAY_QR_CODE_FOR_SCANNING -> {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.display_qr_code),
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

        }
    }
}