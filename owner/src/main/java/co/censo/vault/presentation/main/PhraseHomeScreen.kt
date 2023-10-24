package co.censo.vault.presentation.main

import Base64EncodedData
import StandardButton
import VaultSecretId
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.presentation.SharedColors
import co.censo.vault.R
import kotlinx.datetime.Clock

@Composable
fun PhraseHomeScreen(
    vaultSecrets: List<VaultSecret>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        AddAccessRow(onAddClick = {}, onAccessClick = {})
        Divider(
            modifier = Modifier.height(1.5.dp).fillMaxWidth(),
            color = SharedColors.DividerGray
        )
        Spacer(modifier = Modifier.height(12.dp))
        vaultSecrets.forEach { vaultSecret ->
            Spacer(modifier = Modifier.height(12.dp))
            SeedPhraseItem(vaultSecret = vaultSecret)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AddAccessRow(
    onAddClick: () -> Unit,
    onAccessClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SharedColors.BackgroundGrey.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            StandardButton(
                modifier = Modifier.weight(0.5f),
                color = Color.Black,
                contentPadding = PaddingValues(vertical = 14.dp),
                onClick = onAddClick
            ) {
                Text(
                    text = stringResource(R.string.add),
                    fontSize = 24.sp,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            StandardButton(
                modifier = Modifier.weight(0.5f),
                color = Color.Black,
                contentPadding = PaddingValues(vertical = 14.dp),
                onClick = onAccessClick
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lock_icon),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.access),
                        fontSize = 24.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun SeedPhraseItem(vaultSecret: VaultSecret) {
    Box(
        modifier = Modifier
            .padding(horizontal = 36.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.padding(
                vertical = 32.dp,
                horizontal = 24.dp
            ),
            text = vaultSecret.label,
            fontSize = 28.sp,
            fontWeight = FontWeight.W600,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseHomeScreen() {
    PhraseHomeScreen(
        vaultSecrets = listOf(
            VaultSecret(
                guid = VaultSecretId("12345"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
            VaultSecret(
                guid = VaultSecretId("12345"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
            VaultSecret(
                guid = VaultSecretId("12345"),
                label = "Very very very long name here",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
        )
    )
}