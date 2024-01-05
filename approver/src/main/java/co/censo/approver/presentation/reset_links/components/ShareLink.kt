package co.censo.approver.presentation.reset_links.components

import MessageText
import StandardButton
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ShareLinkUI(
    link: String,
) {
    val context = LocalContext.current

    fun shareLink(link: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        MessageText(
            message = stringResource(R.string.share_recovery_link_message),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 32.dp, bottom = 32.dp),
            onClick = { shareLink(link) },
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = co.censo.shared.R.drawable.share_link),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.share),
                style = ButtonTextStyle.copy(fontSize = 22.sp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShareLinkUIPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        ShareLinkUI(
            link = "censo-reset://reset/token123"
        )
    }
}