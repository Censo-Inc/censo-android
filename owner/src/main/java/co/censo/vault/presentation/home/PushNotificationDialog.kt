package co.censo.vault.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R
import co.censo.vault.ui.theme.DialogMainBackground
import co.censo.vault.ui.theme.TextBlack
import co.censo.vault.ui.theme.UnfocusedGrey

@Composable
fun PushNotificationDialog(
    text: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = UnfocusedGrey.copy(alpha = 0.50f))
                .background(color = DialogMainBackground)
                .shadow(elevation = 2.5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = text,
                textAlign = TextAlign.Center,
                color = TextBlack,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Row {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.skip),
                        fontSize = 18.sp,
                        color = TextBlack,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                    onClick = onAccept,
                ) {
                    Text(
                        text = stringResource(id = R.string.continue_text),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}