package co.censo.censo.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        val itemSpanStyle =
            SpanStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.W400,
                color = SharedColors.MainColorText
            )

        val deleteUserText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.delete_user))
                }
            }

        val signOutText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.sign_out))
                }
            }

        val lockScreenText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.lock))
                }
            }

        val refreshCloudAccessText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.re_sync_drive_access))
                }
            }

        val removeApproversText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.remove_approvers))
                }
            }

        val enableNotificationsText =
            buildAnnotatedString {
                withStyle(itemSpanStyle) {
                    append(stringResource(R.string.allow_push_notifications))
                }
            }

        Spacer(modifier = Modifier.height(44.dp))
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResyncCloudAccess() }
                .padding(
                    horizontal = 24.dp, vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIcon(Icons.Default.CloudSync)
            Spacer(modifier = Modifier.width(8.dp))
            Text(refreshCloudAccessText)
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLock() }
                .padding(
                    horizontal = 24.dp, vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIcon(Icons.Default.Lock)
            Spacer(modifier = Modifier.width(8.dp))
            Text(lockScreenText)
        }

        if (showRemoveApproverButton) {
            Divider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemoveApprover() }
                    .padding(all = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Default.Group)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(removeApproversText)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.an_approval_from_current_approvers_is_required),
                    color = SharedColors.MainColorText,
                    fontSize = 14.sp
                )
            }
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(
                    horizontal = 24.dp, vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIcon(Icons.Default.Logout)
            Spacer(modifier = Modifier.width(8.dp))
            Text(signOutText)
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDeleteUser() }
                .padding(
                    horizontal = 24.dp, vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIcon(Icons.Default.Refresh)
            Spacer(modifier = Modifier.width(8.dp))
            Text(deleteUserText)
        }
        Divider()
        if (showNotificationsButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowPushNotification() }
                    .padding(
                        horizontal = 24.dp, vertical = 24.dp
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Default.Notifications)
                Spacer(modifier = Modifier.width(8.dp))
                Text(enableNotificationsText)
            }
            Divider()
        }
    }
}

@Composable
fun SettingsIcon(imageVector: ImageVector) {
    Icon(
        imageVector,
        contentDescription = "",
        tint = SharedColors.MainIconColor
    )
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

