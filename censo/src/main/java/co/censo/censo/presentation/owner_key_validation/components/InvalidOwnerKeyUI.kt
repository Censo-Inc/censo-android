package co.censo.censo.presentation.owner_key_validation.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun InvalidOwnerKeyUI(
    onInitiateRecovery: () -> Unit,
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
            modifier = Modifier.size(150.dp),
            painter = painterResource(id = R.drawable.erase_text),
            contentDescription = stringResource(co.censo.censo.R.string.failed_to_validate_key),
            colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(co.censo.censo.R.string.censo_was_not_able_to_access_your_key),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = stringResource(co.censo.censo.R.string.collect_approvals_key_recovery_message),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(horizontalSpacingBetweenItems))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp),
            onClick = onInitiateRecovery,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = co.censo.censo.R.string.recover_my_key),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewInvalidApproverKeyUI() {
    InvalidOwnerKeyUI(
        onInitiateRecovery = {}
    )
}
