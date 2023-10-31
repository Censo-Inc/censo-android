package co.censo.guardian.presentation

import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import co.censo.shared.R as SharedR
import co.censo.guardian.R as ApproverR

@Composable
fun LockedApproverScreen(
    onPasteLinkClick: (() -> Unit)? = null
) {

    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .background(color = Color.White)
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val horizontalSpacingBetweenItems = 24.dp

            Image(
                painter = painterResource(id = SharedR.drawable.main_lock),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems + (horizontalSpacingBetweenItems / 2)))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = SharedR.string.data_encrypted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

            MessageText(
                message = ApproverR.string.encrypted_layered_security,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))


            onPasteLinkClick?.let {
                StandardButton(
                    color = Color.Black,
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp),
                    onClick = it,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(
                                id = co.censo.shared.R.drawable.paste_phrase_icon
                            ),
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(ApproverR.string.paste_link),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(
                    id = ApproverR.drawable.active_approvers_icon
                ),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Active approver",
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.W500
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewLockScreenWithButton() {
    LockedApproverScreen {

    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewLockScreenWithoutButton() {
    LockedApproverScreen()
}