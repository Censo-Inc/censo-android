package co.censo.censo.presentation.push_notification

import StandardButton
import TitleText
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.data.Resource
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError

@Composable
fun PushNotificationScreen(
    onFinished: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel()
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

    LaunchedEffect(key1 = state) {

        if (state.showPushNotificationsDialog is Resource.Success) {
            checkNotificationsPermissionDialog()
        }

        if (state.userResponded is Resource.Success) {
            onFinished()
            viewModel.resetUserResponded()
        }
    }


    val verticalSpacingHeight = 24.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = R.string.allow_push_notifications,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.how_censo_communicates),
                fontSize = 20.sp,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.why_censo_sends_notifications),
                fontSize = 20.sp,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight * 2))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.checkUserHasRespondedToNotificationOptIn()
                },
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.enable_notifications),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.setUserResponded()
                },
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.no_thanks),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }

            Spacer(Modifier.height(verticalSpacingHeight))
        }
    }
}