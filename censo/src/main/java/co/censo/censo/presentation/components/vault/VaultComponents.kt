package co.censo.censo.presentation.components.vault

import Base64EncodedData
import SeedPhraseId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generateBase64
import co.censo.shared.data.cryptography.generateHexString
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.SeedPhrase
import co.censo.censo.R
import co.censo.censo.util.TestTag
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SeedPhrasesListItem(
    seedPhrase: SeedPhrase,
    onDelete: (SeedPhrase) -> Unit
) {

    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row {
                Column {

                    Text(
                        text = seedPhrase.label,
                        color = Color.Black,
                        fontSize = 18.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.added_on),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = seedPhrase
                                .createdAt
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .toJavaLocalDateTime()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE),
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { onDelete(seedPhrase) },
                    modifier = Modifier
                        .semantics { testTag = TestTag.delete_phrase },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun AddBip39PhraseUI(
    navigateToAddPhrase: () -> Unit
) {
    TextButton(
        onClick = { navigateToAddPhrase() },
        modifier = Modifier
            .semantics { testTag = TestTag.add_phrase }
            .padding(end = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AddCircle,
            modifier = Modifier.padding(end = 18.dp),
            contentDescription = stringResource(R.string.add_bip39_phrase),
            tint = Color.Black
        )
    }
}

@Preview
@Composable
fun AddBip39PhraseUIPreview() {
    AddBip39PhraseUI {}
}

@Preview
@Composable
fun SeedPhrasesListItemPreview() {
    Box(
        modifier = Modifier.background(color = Color.White)
    ) {
        SeedPhrasesListItem(
            seedPhrase = SeedPhrase(
                guid = SeedPhraseId("seed phrase id"),
                encryptedSeedPhrase = Base64EncodedData(generateBase64()),
                seedPhraseHash = HashedValue(generateHexString()),
                label = "this is a seed phrase",
                createdAt = Clock.System.now()
            ),
            onDelete = {}
        )
    }
}