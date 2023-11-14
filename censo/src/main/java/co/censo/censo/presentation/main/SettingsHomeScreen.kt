package co.censo.censo.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.components.Loading

@Composable
fun SettingsHomeScreen(
    onLock: () -> Unit,
    onDeleteUser: () -> Unit,
    onSignOut: () -> Unit,
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

        Spacer(modifier = Modifier.height(44.dp))
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLock() }
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Lock, contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            Text(lockScreenText)
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Logout, contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            Text(signOutText)
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDeleteUser() }
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            Text(deleteUserText)
        }
        Divider()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreen() {
    SettingsHomeScreen(
        onLock = {},
        onDeleteUser = {},
        onSignOut = {},
    )
}

