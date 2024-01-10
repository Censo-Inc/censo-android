package co.censo.censo.presentation.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.TimelockSetting
import co.censo.shared.presentation.components.SettingsItem
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun SettingsHomeScreen(
    onResyncCloudAccess: () -> Unit,
    onLock: () -> Unit,
    onDeleteUser: () -> Unit,
    onSignOut: () -> Unit,
    showRemoveApproverButton: Boolean,
    onRemoveApprover: () -> Unit,
    onShowPushNotification: () -> Unit,
    showNotificationsButton: Boolean,
    onEnableTimelock: () -> Unit,
    onDisableTimelock: () -> Unit,
    onCancelDisableTimelock: () -> Unit,
    timelockSetting: TimelockSetting
) {

    val context = LocalContext.current

    Column {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_nav_title),
                fontSize = 42.sp,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            Divider()
            SettingsItem(
                title = stringResource(R.string.lock),
                buttonText = stringResource(R.string.lock_button),
                description = stringResource(R.string.lock_description),
                onSelected = onLock
            )
            Divider()
            if (showRemoveApproverButton) {
                SettingsItem(
                    title = stringResource(R.string.remove_approvers),
                    buttonText = stringResource(R.string.remove_approvers_button),
                    description = stringResource(R.string.an_approval_from_current_approvers_is_required),
                    onSelected = onRemoveApprover
                )
                Divider()
            }
            SettingsItem(
                title = stringResource(R.string.re_sync_drive_access),
                buttonText = stringResource(R.string.re_sync_drive_access_button),
                description = stringResource(R.string.re_sync_drive_access_description),
                onSelected = onResyncCloudAccess
            )
            Divider()
            SettingsItem(
                title = stringResource(R.string.sign_out),
                buttonText = stringResource(R.string.sign_out),
                description = stringResource(R.string.sign_out_description),
                onSelected = onSignOut
            )
            Divider()
            when {
                timelockSetting.currentTimelockInSeconds == null ->
                    SettingsItem(
                        title = stringResource(R.string.enable_timelock),
                        buttonText = stringResource(R.string.enable),
                        description = stringResource(
                            R.string.enable_timelock_settings_blurb,
                            formatTimelockDuration(timelockSetting.defaultTimelockInSeconds.seconds, context)
                        ),
                        onSelected = onEnableTimelock
                    )
                timelockSetting.disabledAt != null ->
                    SettingsItem(
                        title = stringResource(R.string.cancel_disable_timelock),
                        buttonText = stringResource(R.string.cancel),
                        description = stringResource(
                            R.string.cancel_disable_timelock_settings_blurb,
                            formatTimelockDuration(timelockSetting.disabledAt!! - Clock.System.now(), context)
                        ),
                        onSelected = onCancelDisableTimelock
                    )
                else ->
                    SettingsItem(
                        title = stringResource(R.string.disable_timelock),
                        buttonText = stringResource(R.string.disable),
                        description = stringResource(
                            R.string.disable_timelock_settings_blurb,
                            formatTimelockDuration(timelockSetting.currentTimelockInSeconds!!.seconds, context)
                        ),
                        onSelected = onDisableTimelock
                    )
            }
            Divider()
            SettingsItem(
                title = stringResource(R.string.delete_user),
                buttonText = stringResource(R.string.delete_user_button),
                description = stringResource(R.string.delete_user_description),
                onSelected = onDeleteUser
            )
            Divider()
            if (showNotificationsButton) {
                SettingsItem(
                    title = stringResource(R.string.allow_push_notifications),
                    buttonText = stringResource(R.string.allow_push_notifications_button),
                    description = stringResource(R.string.allow_push_notifications_description),
                    onSelected = onShowPushNotification
                )
            }
        }
    }
}

fun formatTimelockDuration(duration: Duration, context: Context) =
    when {
        duration < 1.minutes ->
            context.getString(R.string.less_than_a_minute)
        else ->
            (if (duration > 1.hours) duration + 59.minutes else duration + 59.seconds).toComponents { hours, minutes, _, _ ->
                if (hours >= 1L) {
                    "$hours ${context.getString(if (hours == 1L) R.string.hour else R.string.hours)}"
                } else {
                    "$minutes ${context.getString(R.string.minutes)}"
                }
            }
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreenWithoutRemoveApprover() {
    SettingsHomeScreen(
        onResyncCloudAccess = {},
        onLock = {},
        onDeleteUser = {},
        onSignOut = {},
        onRemoveApprover = {},
        showRemoveApproverButton = false,
        onShowPushNotification = {},
        showNotificationsButton = true,
        onEnableTimelock = {},
        onDisableTimelock = {},
        onCancelDisableTimelock = {},
        timelockSetting = TimelockSetting(3700L, null, null)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreenWithRemoveApprover() {
    SettingsHomeScreen(
        onResyncCloudAccess = {},
        onLock = {},
        onDeleteUser = {},
        onSignOut = {},
        onRemoveApprover = {},
        showRemoveApproverButton = true,
        onShowPushNotification = {},
        showNotificationsButton = true,
        onEnableTimelock = {},
        onDisableTimelock = {},
        onCancelDisableTimelock = {},
        timelockSetting = TimelockSetting(3550L, 7200L, null)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreenWithPendingDisableTimelock() {
    SettingsHomeScreen(
        onResyncCloudAccess = {},
        onLock = {},
        onDeleteUser = {},
        onSignOut = {},
        onRemoveApprover = {},
        showRemoveApproverButton = true,
        onShowPushNotification = {},
        showNotificationsButton = true,
        onEnableTimelock = {},
        onDisableTimelock = {},
        onCancelDisableTimelock = {},
        timelockSetting = TimelockSetting(3600L, 7200L, Clock.System.now().plus(5.minutes))
    )
}

