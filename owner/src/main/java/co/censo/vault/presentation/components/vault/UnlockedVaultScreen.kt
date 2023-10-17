package co.censo.vault.presentation.components.vault

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors

@Composable
fun UnlockedVaultScreen(
    onEditSeedPhrases: () -> Unit,
    onRecoverSeedPhrases: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = VaultColors.PrimaryColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.your_seed_phrases_are_protected),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.W700
        )

        Spacer(modifier = Modifier.weight(1f))

        StandardButton(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = VaultColors.PrimaryColor,
            borderColor = Color.White,
            border = true,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
            onClick = onEditSeedPhrases,
        ) {
            Text(
                text = stringResource(R.string.edit_seed_phrases),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StandardButton(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.White,
            borderColor = Color.White,
            border = true,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
            onClick = onRecoverSeedPhrases,
        ) {
            Text(
                text = stringResource(R.string.recover_phrases),
                color = VaultColors.PrimaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700
            )
        }

        Spacer(modifier = Modifier.weight(1f))

    }
}

@Preview
@Composable
fun UnlockedVaultScreenPreview() {
    UnlockedVaultScreen(
        onEditSeedPhrases = {},
        onRecoverSeedPhrases = {}
    )
}