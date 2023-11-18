package co.censo.approver.presentation.components

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
import co.censo.approver.R as ApproverR

@Composable
fun PasteLink(
    onPasteLinkClick: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val verticalSpacingBetweenItems = 24.dp

        Image(
            painter = painterResource(id = ApproverR.drawable.main_export_link),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.get_unique_link,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.once_receive_it,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.or_simply_copy_link,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onPasteLinkClick,
        ) {
            Text(
                text = stringResource(ApproverR.string.paste_link),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PasteLinkPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)) {
        PasteLink(
            onPasteLinkClick = {}
        )
    }
}