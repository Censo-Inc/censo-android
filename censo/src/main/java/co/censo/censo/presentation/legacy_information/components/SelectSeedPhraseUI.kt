package co.censo.censo.presentation.legacy_information.components

import Base64EncodedData
import SeedPhraseId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.presentation.main.SeedPhraseItem
import co.censo.shared.data.model.SeedPhraseEncryptedNotes
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.PhraseType
import co.censo.shared.data.model.SeedPhrase
import kotlinx.datetime.Clock

@Composable
fun SelectSeedPhraseUI(
    seedPhrases: List<SeedPhrase>,
    onSeedPhraseSelected: (SeedPhrase) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.025f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                seedPhrases.forEach { seedPhrase ->
                    Spacer(modifier = Modifier.height(12.dp))
                    SeedPhraseItem(
                        seedPhrase = seedPhrase,
                        hasNotes = seedPhrase.encryptedNotes != null,
                        onClick = {
                            onSeedPhraseSelected(seedPhrase)
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeSelectSeedPhraseUI() {
    SelectSeedPhraseUI(
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Photo,
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Binary,
                encryptedNotes = SeedPhraseEncryptedNotes(
                    masterKeyEncryptedText = Base64EncodedData(""),
                    ownerApproverKeyEncryptedText = Base64EncodedData(""),
                ),
            )
        ),
        onSeedPhraseSelected = {},
    )
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumSelectSeedPhraseUI() {
    SelectSeedPhraseUI(
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Photo,
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Binary,
                encryptedNotes = SeedPhraseEncryptedNotes(
                    masterKeyEncryptedText = Base64EncodedData(""),
                    ownerApproverKeyEncryptedText = Base64EncodedData(""),
                ),
            )
        ),
        onSeedPhraseSelected = {},
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallSelectSeedPhraseUI() {
    SelectSeedPhraseUI(
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Photo,
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                createdAt = Clock.System.now(),
                type = PhraseType.Binary,
                encryptedNotes = SeedPhraseEncryptedNotes(
                    masterKeyEncryptedText = Base64EncodedData(""),
                    ownerApproverKeyEncryptedText = Base64EncodedData(""),
                ),
            )
        ),
        onSeedPhraseSelected = {},
    )
}