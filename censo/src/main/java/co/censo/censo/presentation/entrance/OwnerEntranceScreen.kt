package co.censo.censo.presentation.entrance

import MessageText
import ParticipantId
import StandardButton
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.R as SharedR
import co.censo.censo.R as CensoR
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.termsOfUseVersions
import co.censo.shared.data.model.touVersion
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.LinksUtil
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
            state.navigationResource.data?.let { destination ->
                navController.navigate(destination) {
                    popUpTo(destination) {
                        inclusive = true
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
                TermsOfUse {
                    viewModel.setAcceptedTermsOfUseVersion(touVersion)
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
                    authenticate = { viewModel.startGoogleSignInFlow() }
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
) {
    val uriHandler = LocalUriHandler.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        Image(
            painter = painterResource(id = co.censo.censo.R.drawable.censo_login_logo),
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


        Spacer(modifier = Modifier.weight(1.0f))

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
                    painter = painterResource(id = co.censo.censo.R.drawable.safe_icon),
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TermsOfUse(
    onAccept: () -> Unit
) {

    var isReview by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isReview) {
                HtmlText(
                    termsOfUseVersions[touVersion]!!,
                    Modifier
                        .padding(paddingValues)
                        .fillMaxHeight(0.8f)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Image(
                    painterResource(id = SharedR.drawable.files),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = SharedColors.LoginIconColor)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(SharedR.string.terms_of_use),
                    fontWeight = FontWeight.W600,
                    fontSize = 24.sp,
                    color = SharedColors.MainColorText
                )
                Spacer(modifier = Modifier.height(24.dp))
                MessageText(
                    message = SharedR.string.tou_blurb,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onClick = { isReview = true },
                ) {
                    Text(
                        text = stringResource(id = SharedR.string.tou_review),
                        style = ButtonTextStyle.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(all = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onAccept,
            ) {
                Text(
                    text = stringResource(SharedR.string.tou_accept),
                    modifier = Modifier.padding(all = 8.dp),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(SharedR.string.tou_agreement),
                fontSize = 11.sp,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, @ColorInt color: Int = android.graphics.Color.parseColor("#000000")) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context).apply {
            setTextColor(color)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } },
        update = {
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun TermsOfUsePreview() {
    TermsOfUse {
        print("Accepted!")
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OwnerEntranceStandardUIPreview() {
    Surface {
        OwnerEntranceStandardUI({})
    }
}
