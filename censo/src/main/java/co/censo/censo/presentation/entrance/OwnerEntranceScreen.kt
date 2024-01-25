package co.censo.censo.presentation.entrance

import ParticipantId
import StandardButton
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.components.TermsOfUse
import co.censo.shared.data.Resource
import co.censo.shared.R as SharedR
import co.censo.censo.R as CensoR
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.touVersion
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.LinksUtil
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OwnerEntranceScreen(
    navController: NavController,
    viewModel: OwnerEntranceViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val state = viewModel.state

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> {
                    viewModel.googleAuthCancel()
                }

                else -> {
                    viewModel.googleAuthFailure(GoogleAuthError.IntentResultFailed)
                }
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.userFinishedSetup && !state.showAcceptTermsOfUse) {
            viewModel.retrieveOwnerStateAndNavigate()
            viewModel.resetUserFinishedSetup()
        }

        if (state.triggerGoogleSignIn is Resource.Success) {
            try {
                val googleSignInClient = viewModel.getGoogleSignInClient()

                val intent = googleSignInClient.signInIntent
                googleAuthLauncher.launch(intent)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SignIn)
                viewModel.googleAuthFailure(GoogleAuthError.FailedToLaunchGoogleAuthUI(e))
            }
            viewModel.resetTriggerGoogleSignIn()
        }

        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route) {
                    if (navigationData.popSelfFromBackStack) {
                        popCurrentDestinationFromBackStack(navController)
                    }
                }
            }
            viewModel.resetNavigationResource()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {

        when {
            state.showAcceptTermsOfUse -> {
                if (state.deleteUserResource is Resource.Error) {
                    DisplayError(
                        errorMessage = state.deleteUserResource.getErrorMessage(context),
                        dismissAction = viewModel::resetDeleteUserResource,
                    ) { viewModel.deleteUser() }
                } else {
                    TermsOfUse(
                        onAccept = {
                            viewModel.setAcceptedTermsOfUseVersion(touVersion)
                        },
                        onCancel = viewModel::showDeleteUserDialog,
                        viewModel.state.userIsOnboarding
                    )
                    if (state.triggerDeleteUserDialog is Resource.Success) {
                        ConfirmationDialog(
                            title = stringResource(id = R.string.exit_setup),
                            message = stringResource(R.string.exit_setup_details),
                            onCancel = viewModel::onCancelResetUser,
                            onDelete = viewModel::deleteUser,
                        )
                    }
                }
            }

            state.isLoading -> LargeLoading(
                fullscreen = true,
                fullscreenBackgroundColor = Color.White
            )


            state.apiCallErrorOccurred -> {
                if (state.signInUserResource is Resource.Error) {
                    DisplayError(
                        errorMessage = state.signInUserResource.getErrorMessage(context),
                        dismissAction = viewModel::resetSignInUserResource,
                    ) { viewModel.retrySignIn() }
                } else if (state.triggerGoogleSignIn is Resource.Error) {
                    DisplayError(
                        errorMessage = state.triggerGoogleSignIn.getErrorMessage(context),
                        dismissAction = viewModel::resetTriggerGoogleSignIn,
                    ) { viewModel.retrySignIn() }
                } else if (state.userResponse is Resource.Error) {
                    DisplayError(
                        errorMessage = state.userResponse.getErrorMessage(context),
                        dismissAction = viewModel::retrieveOwnerStateAndNavigate,
                    ) { viewModel.retrieveOwnerStateAndNavigate() }
                }
            }

            else -> {
                OwnerEntranceStandardUI(
                    authenticate = { viewModel.startGoogleSignInFlow() },
                    recover = { viewModel.startLoginIdRecovery() },
                )
            }
        }

        if (state.forceUserToGrantCloudStorageAccess.requestAccess) {
            CloudStorageHandler(
                actionToPerform = CloudStorageActions.ENFORCE_ACCESS,
                participantId = ParticipantId(""),
                encryptedPrivateKey = null,
                onActionSuccess = {},
                onActionFailed = {},
                onCloudStorageAccessGranted = { viewModel.handleCloudStorageAccessGranted() }
            )
        }
    }
}

@Composable
fun OwnerEntranceStandardUI(
    authenticate: () -> Unit,
    recover: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val resetTag = "reset"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        Image(
            modifier = Modifier.height(120.dp),
            painter = painterResource(id = R.drawable.censo_logo_dark_blue_stacked),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.weight(0.25f))
        Text(
            text = stringResource(SharedR.string.tag_line),
            fontWeight = FontWeight.W600,
            fontSize = 22.sp,
            color = SharedColors.MainColorText
        )
        Spacer(modifier = Modifier.weight(0.50f))
        StandardButton(
            onClick = authenticate,
            coolDownDuration = 500.milliseconds,
            color = Color.Black,
            contentPadding = PaddingValues(
                horizontal = 48.dp,
                vertical = 16.dp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = SharedR.drawable.google),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(SharedR.string.google_auth_login),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        val basicStyle = SpanStyle(
            color = SharedColors.MainColorText,
        )
        val recoveryText = buildAnnotatedString {
            withStyle(basicStyle) {
                append(stringResource(R.string.need_to_reset_login_id))
            }
            pushStringAnnotation(tag = resetTag, annotation = "Reset Login ID")
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(stringResource(R.string.here))
            }
            pop()
            withStyle(basicStyle) {
                append(".")
            }
        }
        ClickableText(
            text = recoveryText,
            style = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
            onClick = { offset ->
                recoveryText.getStringAnnotations(tag = resetTag, start = offset, end = offset)
                    .firstOrNull()?.let {
                        recover()
                    }
            },
        )

        Spacer(modifier = Modifier.weight(0.7f))

        Text(
            modifier = Modifier.padding(horizontal = 44.dp),
            fontSize = 13.sp,
            text = stringResource(SharedR.string.sign_in_google_explainer),
            lineHeight = 14.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(0.25f))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(0.1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = CensoR.drawable.eye_slash_icon),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(SharedR.string.no_personal_info),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = SharedColors.MainColorText,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.safe_icon),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(SharedR.string.multiple_layers),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = SharedColors.MainColorText,
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
        Divider(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp))
        Row(
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 36.dp,
                    bottom = 36.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = stringResource(SharedR.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.TERMS_URL) },
                fontWeight = FontWeight.SemiBold,
                color = SharedColors.MainColorText,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(SharedR.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.PRIVACY_URL) },
                fontWeight = FontWeight.SemiBold,
                color = SharedColors.MainColorText,
                fontSize = 16.sp
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OwnerEntranceStandardUIPreview() {
    OwnerEntranceStandardUI(
        authenticate = {},
        recover = {},
    )
}
