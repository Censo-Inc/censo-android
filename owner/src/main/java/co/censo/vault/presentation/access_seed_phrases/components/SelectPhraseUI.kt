package co.censo.vault.presentation.access_seed_phrases.components

import Base64EncodedData
import LearnMore
import StandardButton
import TitleText
import VaultSecretId
import android.content.IntentSender.OnFinished
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import co.censo.shared.data.model.VaultSecret
import co.censo.vault.R
import co.censo.vault.presentation.main.SeedPhraseItem
import kotlinx.datetime.Clock

@Composable
fun SelectPhraseUI(
    vaultSecrets: List<VaultSecret>,
    onPhraseSelected: (VaultSecret) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TitleText(
            modifier = Modifier.padding(12.dp),
            title = R.string.select_seed_phrase
        )
        Spacer(modifier = Modifier.height(12.dp))

        vaultSecrets.forEach {
            Spacer(modifier = Modifier.height(12.dp))
            SeedPhraseItem(
                horizontalPadding = 0.dp,
                vaultSecret = it
            ) {
                onPhraseSelected(it)
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
            color = Color.Black
        ) {
            Text(
                text = stringResource(id = R.string.finish),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                modifier = Modifier.padding(all = 8.dp)
            )
        }

        LearnMore {

        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSelectPhraseUI() {
    SelectPhraseUI(
        vaultSecrets = listOf(
            VaultSecret(
                guid = VaultSecretId(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                seedPhraseHash = HashedValue(""),
                label = "Yankee Hotel Foxtrot",
                createdAt = Clock.System.now(),
            ),
            VaultSecret(
                guid = VaultSecretId(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                seedPhraseHash = HashedValue(""),
                label = "BashOLantern",
                createdAt = Clock.System.now(),
            ),
            VaultSecret(
                guid = VaultSecretId(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                seedPhraseHash = HashedValue(""),
                label = "Robin Hood",
                createdAt = Clock.System.now(),
            ),

            ),
        onPhraseSelected = {},
        onFinish = {}
    )
}