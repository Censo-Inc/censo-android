package co.censo.vault.presentation.lock_screen.components

import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R

@Composable
fun LockEngagedUI(
    initUnlock: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val horizontalSpacingBetweenItems = 24.dp

        Image(
            painter = painterResource(id = co.censo.shared.R.drawable.main_lock),
            contentDescription = stringResource(R.string.app_content_is_locked_behind_facescan)
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems + (horizontalSpacingBetweenItems / 2)))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = co.censo.shared.R.string.data_encrypted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

        MessageText(
            message = R.string.encrypted_your_data_behind,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

        StandardButton(
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp),
            onClick = initUnlock,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(
                        id = co.censo.shared.R.drawable.small_face_scan
                    ),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.face_scan_to_unlock),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewLockScreen() {
    LockEngagedUI {

    }
}
