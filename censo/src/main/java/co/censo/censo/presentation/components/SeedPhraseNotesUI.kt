package co.censo.censo.presentation.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

enum class SeedPhraseNotesUIEntryPoint {
    LegacyInformation, AddSeedPhrase, EditSeedPhrase
}

@Composable
fun SeedPhraseNotesUI(
    notes: String,
    onContinue: (String) -> Unit,
    entryPoint: SeedPhraseNotesUIEntryPoint,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.025f

    val seedPhraseInfoState = remember { mutableStateOf(notes)}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .border(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    shape = RoundedCornerShape(12.dp)
                ),
            value = seedPhraseInfoState.value,
            singleLine = false,
            onValueChange = { newValue ->
                seedPhraseInfoState.value = newValue
            },
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = SharedColors.MainColorText
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 14.dp)
                ) {

                    if (seedPhraseInfoState.value.isEmpty()) {
                        Text(
                            text = when (entryPoint) {
                                SeedPhraseNotesUIEntryPoint.LegacyInformation -> stringResource(R.string.seed_phrase_notes_legacy_placeholder_text)

                                SeedPhraseNotesUIEntryPoint.AddSeedPhrase,
                                SeedPhraseNotesUIEntryPoint.EditSeedPhrase -> stringResource(R.string.seed_phrase_notes_owner_placeholder_text)
                            },
                            style = TextStyle(
                                fontSize = 20.sp,
                                color = Color.Gray
                            )
                        )
                    }

                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onContinue(seedPhraseInfoState.value) },
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(
                text = when (entryPoint) {
                    SeedPhraseNotesUIEntryPoint.AddSeedPhrase -> stringResource(R.string.continue_text)

                    SeedPhraseNotesUIEntryPoint.LegacyInformation,
                    SeedPhraseNotesUIEntryPoint.EditSeedPhrase -> stringResource(R.string.save)
                },
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.W400)
            )
        }
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        Spacer(modifier = Modifier.weight(0.2f))
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeSeedPhraseNotesUI() {
    SeedPhraseNotesUI(
        notes = "Yankee Hotel Foxtrot",
        onContinue = {},
        entryPoint = SeedPhraseNotesUIEntryPoint.LegacyInformation,
    )
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumSeedPhraseNotesUI() {
    SeedPhraseNotesUI(
        notes = "Yankee Hotel Foxtrot",
        onContinue = {},
        entryPoint = SeedPhraseNotesUIEntryPoint.AddSeedPhrase,
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallSeedPhraseNotesUI() {
    SeedPhraseNotesUI(
        notes = "Yankee Hotel Foxtrot",
        onContinue = {},
        entryPoint = SeedPhraseNotesUIEntryPoint.EditSeedPhrase,
    )
}