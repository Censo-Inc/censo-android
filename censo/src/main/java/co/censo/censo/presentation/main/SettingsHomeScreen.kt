package co.censo.censo.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@Composable
fun SettingsHomeScreen(
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
                fontSize = 20.sp,
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

        Spacer(modifier = Modifier.height(72.dp))
        Divider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDeleteUser() }
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
        ) {
            Text(deleteUserText)
        }
        Divider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
        ) {
            Text(signOutText)
        }
        Divider()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreen() {
    SettingsHomeScreen(
        onDeleteUser = {},
        onSignOut = {},
    )
}
