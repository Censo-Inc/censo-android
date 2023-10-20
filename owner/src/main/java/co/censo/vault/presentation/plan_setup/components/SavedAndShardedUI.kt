package co.censo.vault.presentation.plan_setup.components

import LearnMore
import MessageText
import StandardButton
import SubTitleText
import TitleText
import android.app.backup.BackupAgent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import co.censo.shared.util.projectLog
import co.censo.vault.R

@Composable
fun SavedAndShardedUI(
    seedPhraseNickname: String,
    primaryApproverNickname: String,
    backupApproverNickname: String,
) {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        Image(
            painterResource(id = R.drawable.check_circle),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.saved_sharded,
        )

        if (seedPhraseNickname.isNotBlank()) {
            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = seedPhraseNickname,
                fontWeight = FontWeight.W400
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(color = Color.Black)
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = "You",
            fontWeight = FontWeight.W400
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = primaryApproverNickname,
            fontWeight = FontWeight.W400
        )

        if (backupApproverNickname.isNotBlank()) {
            Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = backupApproverNickname,
                fontWeight = FontWeight.W400
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 100.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SavedAndShardedUIWithSeedPhrasePreview() {
    SavedAndShardedUI(
        seedPhraseNickname = "Yankee Hotel Foxtrot",
        primaryApproverNickname = "Neo",
        backupApproverNickname = "John Wick"
    )
}

@Preview(showBackground = true)
@Composable
fun SavedAndShardedUIWithoutSeedPhrasePreview() {
    SavedAndShardedUI(
        seedPhraseNickname = "",
        primaryApproverNickname = "Neo",
        backupApproverNickname = "John Wick"
    )
}