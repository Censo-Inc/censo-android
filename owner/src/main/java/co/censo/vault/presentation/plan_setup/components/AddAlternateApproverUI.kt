package co.censo.vault.presentation.plan_setup.components

import LearnMore
import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R

@Composable
fun AddAlternateApproverUI(
    onInviteAlternateSelected: () -> Unit,
    onSaveAndFinishSelected: () -> Unit
) {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.add_an_alternate_approver,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))


        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.add_an_alternate_approver_message,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onInviteAlternateSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.approvers),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.invite_alternate),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onSaveAndFinishSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.save_finish),
                color = Color.White,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun AddAlternateApproverUIPreview() {
    AddAlternateApproverUI(
        onInviteAlternateSelected = {},
        onSaveAndFinishSelected = {},
    )
}