package co.censo.censo.presentation.access_seed_phrases.components

import StandardButton
import TitleText
import SeedPhraseId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.SeedPhrase
import co.censo.censo.R
import co.censo.censo.presentation.main.SeedPhraseItem
import co.censo.shared.presentation.ButtonTextStyle
import kotlinx.datetime.Clock

@Composable
fun SelectPhraseUI(
    seedPhrases: List<SeedPhrase>,
    viewedIds: List<SeedPhraseId>,
    onPhraseSelected: (SeedPhrase) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TitleText(
            modifier = Modifier.padding(12.dp),
            title = R.string.select_seed_phrase
        )
        Spacer(modifier = Modifier.height(12.dp))

        seedPhrases.forEach { seedPhrase ->
            Spacer(modifier = Modifier.height(12.dp))
            SeedPhraseItem(
                seedPhrase = seedPhrase,
                isSelected = viewedIds.any { it == seedPhrase.guid}
            ) {
                onPhraseSelected(seedPhrase)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            onClick = onFinish,
        ) {
            Text(
                text = stringResource(id = R.string.exit_accessing_phrase),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(all = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSelectPhraseUI() {
    SelectPhraseUI(
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                seedPhraseHash = HashedValue(""),
                label = "Yankee Hotel Foxtrot",
                createdAt = Clock.System.now(),
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                seedPhraseHash = HashedValue(""),
                label = "Robin Hood",
                createdAt = Clock.System.now(),
            ),
            SeedPhrase(
                guid = SeedPhraseId("3"),
                label = "SEED PHRASE WITH A VERY LONG NAME OF 50 CHARACTERS",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now()
            ),
        ),
        viewedIds = listOf(
            SeedPhraseId("2"),
        ),
        onPhraseSelected = {},
        onFinish = {}
    )
}