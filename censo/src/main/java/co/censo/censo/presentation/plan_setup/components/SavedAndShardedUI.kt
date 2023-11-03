package co.censo.censo.presentation.plan_setup.components

import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.R

@Composable
fun SavedAndShardedUI(
    seedPhraseNickname: String?,
    primaryApproverNickname: String?,
    alternateApproverNickname: String?,
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
            painterResource(id = co.censo.shared.R.drawable.check_circle),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.saved_sharded,
        )

        if (!seedPhraseNickname.isNullOrBlank()) {
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

        if (!primaryApproverNickname.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = primaryApproverNickname,
                fontWeight = FontWeight.W400
            )
        }

        if (!alternateApproverNickname.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(verticalSpacingHeight * 0.5f))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = alternateApproverNickname,
                fontWeight = FontWeight.W400
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 100.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SavedAndShardedUINoSeedPhrasePreview() {
    SavedAndShardedUI(
        seedPhraseNickname = null,
        primaryApproverNickname = "Neo",
        alternateApproverNickname = "John Wick"
    )
}

@Preview(showBackground = true)
@Composable
fun SavedAndShardedUSingleSeedPhrasePreview() {
    SavedAndShardedUI(
        seedPhraseNickname = "Yankee Hotel Foxtrot",
        primaryApproverNickname = "Neo",
        alternateApproverNickname = "John Wick"
    )
}



@Preview(showBackground = true)
@Composable
fun SavedAndShardedUIMultipleSeedPhrasesPreview() {
    SavedAndShardedUI(
        seedPhraseNickname = "3 seed phrases",
        primaryApproverNickname = "Neo",
        alternateApproverNickname = "John Wick"
    )
}