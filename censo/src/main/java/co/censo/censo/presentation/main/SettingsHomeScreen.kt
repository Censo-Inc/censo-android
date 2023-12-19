package co.censo.censo.presentation.main

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.InvertedButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SettingsHomeScreen(
    onResyncCloudAccess: () -> Unit,
    onLock: () -> Unit,
    onDeleteUser: () -> Unit,
    onSignOut: () -> Unit,
    showRemoveApproverButton: Boolean,
    onRemoveApprover: () -> Unit,
    onShowPushNotification: () -> Unit,
    showNotificationsButton: Boolean
) {
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

@Composable
fun SettingsItem(
    title: String,
    buttonText: String,
    description: String?,
    onSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                modifier = Modifier.weight(1.0f)
            )
            Spacer(Modifier.weight(0.05f))
            StandardButton(
                color = SharedColors.ButtonTextBlue,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                onClick = {
                    onSelected()
                },
                modifier = Modifier.weight(0.5f)
            ) {
                Text(
                    text = buttonText,
                    style = InvertedButtonTextStyle,
                    maxLines = 1,
                )
            }
        }
        description?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = SharedColors.MainColorText,
                fontSize = 14.sp
            )
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
        showNotificationsButton = true
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
        showNotificationsButton = true
    )
}

