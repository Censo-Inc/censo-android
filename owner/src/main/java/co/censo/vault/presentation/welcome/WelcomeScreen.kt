package co.censo.vault.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.Screen

@Composable
fun WelcomeScreen(
    navController: NavController,
    viewModel: WelcomeViewModel = hiltViewModel()
) {

    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

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
            if (state.ownerStateResource is Resource.Error) {
                DisplayError(
                    errorMessage = state.ownerStateResource.getErrorMessage(context),
                    dismissAction = null,
                    retryAction = viewModel::retrieveOwnerState
                )
            }
        }

        else -> {
            state.ownerStateResource.data?.also { ownerState ->

                when (ownerState) {
                    is OwnerState.Initial, is OwnerState.GuardianSetup -> {
                        WelcomeScreenUI {
                            navController.navigate(Screen.InitialPlanSetupRoute.route)
                        }
                    }

                    is OwnerState.Ready -> {
                        if (ownerState.vault.secrets.isEmpty()) {
                            WelcomeScreenUI {
                                navController.navigate(
                                    Screen.EnterPhraseRoute.buildNavRoute(
                                        masterPublicKey = ownerState.vault.publicMasterEncryptionKey,
                                        welcomeFlow = true
                                    )
                                )
                            }
                        } else {
                            WelcomeScreenUI {
                                //todo: Put back in the add approvers flow...
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupStep(
    imagePainter: Painter,
    heading: String,
    content: String,
    completionText: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(color = Color.LightGray, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = heading, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(text = content, fontSize = 14.sp)
            if (completionText != null) {
                Text(
                    text = "✓ $completionText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedColors.SuccessGreen,
                )
            }
        }
    }
}

@Composable
fun WelcomeScreenUI(
    navigateToPlanSetup: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.welcome_to_censo),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .padding(all = 32.dp)
                .width(IntrinsicSize.Max)
        )
        Text(
            stringResource(id = R.string.welcome_blurb),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Left,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(32.dp)
        ) {
            SetupStep(
                painterResource(id = co.censo.shared.R.drawable.google),
                stringResource(R.string.authenticate_privately),
                stringResource(R.string.authenticate_privately_blurb),
                stringResource(R.string.authenticated)
            )
            SetupStep(
                painterResource(id = co.censo.shared.R.drawable.small_face_scan),
                stringResource(id = R.string.scan_your_face),
                stringResource(id = R.string.scan_your_face_blurb),
            )
            SetupStep(
                painterResource(id = co.censo.shared.R.drawable.phrase_entry),
                stringResource(id = R.string.enter_your_phrase),
                stringResource(id = R.string.enter_your_phrase_blurb),
            )
            Divider()
            SetupStep(
                painterResource(id = co.censo.shared.R.drawable.two_people),
                stringResource(id = R.string.add_approvers),
                stringResource(id = R.string.add_approvers_blurb),
            )
            Divider()
        }
        Button(
            onClick = navigateToPlanSetup,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.get_started),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(all = 8.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.info),
                modifier = Modifier
                    .height(26.dp)
                    .padding(6.dp)
            )
            Text(stringResource(id = R.string.info))
        }
    }
}

@Preview
@Composable
fun WelcomeScreenUIPreview() {
    WelcomeScreenUI {

    }
}