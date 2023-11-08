package co.censo.shared.presentation.entrance

import ParticipantId
import StandardButton
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.R
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.termsOfUseVersions
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EntranceScreen(
    navController: NavController,
    viewModel: EntranceViewModel = hiltViewModel()
) {
    val context = LocalContext.current as FragmentActivity

    val state = viewModel.state

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.finishPushNotificationDialog()
        }
    )

    fun checkNotificationsPermissionDialog() {
        try {
            val notificationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (notificationGranted != PackageManager.PERMISSION_GRANTED) {
                val shownPermissionBefore =
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                val seenDialogBefore = viewModel.userHasSeenPushDialog()

                if (!shownPermissionBefore && !seenDialogBefore) {
                    viewModel.setUserSeenPushDialog(true)
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.PermissionDialog)
        }
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> {
                    viewModel.googleAuthFailure(GoogleAuthError.UserCanceledGoogleSignIn)
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
        if (state.userFinishedSetup is Resource.Success) {
            if (!state.showAcceptTermsOfUse) {
                state.userFinishedSetup.data?.let {
                    navController.navigate(it)
                }
                viewModel.resetUserFinishedSetup()
            }
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

        if (state.showPushNotificationsDialog is Resource.Success) {
            checkNotificationsPermissionDialog()
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
                    viewModel.setAcceptedTermsOfUseVersion("v0.1")
                }
            }

            state.isLoading -> {
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
                        color = Color.Black
                    )
                }
            }

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
                }
            }

            else -> {
                OwnerEntranceStandardUI(
                    authenticate = { viewModel.startGoogleSignInFlow() }
                )

                if (state.forceUserToGrantCloudStorageAccess.requestAccess) {
                    CloudStorageHandler(
                        actionToPerform = CloudStorageActions.ENFORCE_ACCESS,
                        participantId = ParticipantId(""),
                        privateKey = null,
                        onActionSuccess = {},
                        onActionFailed = {},
                        onCloudStorageAccessGranted = { viewModel.handleCloudStorageAccessGranted() }
                    )
                }
            }
        }
    }
}

@Composable
fun OwnerEntranceStandardUI(
    authenticate: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(id = R.drawable.censo_text),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sensible_crypto_security),
            fontWeight = FontWeight.W600,
            fontSize = 24.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        StandardButton(
            onClick = authenticate,
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
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.google_auth_login),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = 10.dp)
        ) {
            Icon(
                Icons.Outlined.Info, contentDescription = null,
                Modifier
                    .height(height = 34.dp)
                    .padding(all = 8.dp)
            )
            Text(
                text = stringResource(R.string.why_google),
                fontSize = 20.sp,
                fontWeight = FontWeight.W500
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

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
                    painter = painterResource(id = R.drawable.eyeslash),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(R.string.no_personal_info),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.safe),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(R.string.multiple_layers),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
        Divider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        Row(
            modifier = Modifier.padding(
                start = 32.dp,
                end = 32.dp,
                top = 20.dp,
                bottom = 32.dp
            )
        ) {
            Text(
                text = stringResource(R.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/legal/terms") },
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = stringResource(R.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/legal/privacy") },
                fontWeight = FontWeight.SemiBold
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

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = SharedColors.BackgroundGrey
            ),
            title = {
                Text(
                    stringResource(R.string.terms_of_use),
                )
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isReview) {
                HtmlText(
                    termsOfUseVersions["v0.1"]!!,
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
                    painterResource(id = R.drawable.files),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.terms_of_use),
                    fontWeight = FontWeight.W600,
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.tou_blurb),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.LightGray,
                            offset = Offset(0f, 10f),
                            blurRadius = 10f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onClick = { isReview = true },
                    color = Color.Black
                ) {
                    Text(
                        text = stringResource(id = R.string.tou_review),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(all = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onAccept,
                color = Color.Black
            ) {
                Text(
                    text = stringResource(R.string.tou_accept),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(all = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.tou_agreement),
                fontSize = 11.sp,
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

@Preview
@Composable
fun TermsOfUsePreview() {
    TermsOfUse {
        print("Accepted!")
    }
}

@Preview()
@Composable
fun OwnerEntranceStandardUIPreview() {
    Surface {
        OwnerEntranceStandardUI({})
    }
}
