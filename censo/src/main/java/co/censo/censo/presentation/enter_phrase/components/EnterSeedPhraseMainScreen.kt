package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import androidx.compose.foundation.layout.Column
import MessageText
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.components.LanguageSelectionMenu
import co.censo.shared.util.BIP39
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SelectSeedPhraseEntryType(
    welcomeFlow: Boolean,
    savedPhrases: Int,
    currentLanguage: BIP39.WordListLanguage,
    onManualEntrySelected: (selectedLanguage: BIP39.WordListLanguage) -> Unit,
    onPasteEntrySelected: () -> Unit,
    onGenerateEntrySelected: (selectedLanguage: BIP39.WordListLanguage) -> Unit,
    userHasOwnPhrase: Boolean,
    onUserHasOwnPhrase: () -> Unit,
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    val title = when {
        userHasOwnPhrase -> R.string.how_to_provide
        welcomeFlow -> R.string.time_to_add_your_first_seed_phrase
        else -> R.string.add_another_seed_phrase
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.020f

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
        Image(
            modifier = Modifier
                .padding(start = screenWidth * 0.10f, top = screenHeight * 0.05f)
                .weight(0.40f),
            painter = painterResource(id = R.drawable.add_your_seed_phrase),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.weight(0.35f))
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.weight(0.3f))

        Column(
            modifier = Modifier
                .weight(0.7f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            TitleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                title = title,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            val basicStyle = SpanStyle(
                color = SharedColors.MainColorText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )

            if (savedPhrases == 1) {
                val freePhrase = buildAnnotatedString {
                    withStyle(basicStyle) {
                        append(stringResource(R.string.storing_one_phrase_free))
                    }
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                    text = freePhrase
                )
                Spacer(modifier = Modifier.height(verticalSpacingHeight))
            }

            val languageSelectionText = buildAnnotatedString {
                withStyle(basicStyle) {
                    if (!userHasOwnPhrase) {
                        if (welcomeFlow) {
                            append(stringResource(R.string.generate_or_add_own_message))
                        } else {
                            append(stringResource(R.string.add_another_phrase))
                        }
                        append(
                            stringResource(
                                R.string.current_language,
                                selectedLanguage.displayName()
                            )
                        )
                    } else {
                        append(
                            stringResource(
                                R.string.current_language_own_phrase,
                                selectedLanguage.displayName()
                            )
                        )
                    }
                }
                withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                    append(stringResource(R.string.here))
                }
            }

            LanguageSelectionMenu(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                text = languageSelectionText,
                currentLanguage = selectedLanguage,
                action = {
                    selectedLanguage = it
                }
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight * 2))

            if (!userHasOwnPhrase) {
                SelectPhraseCreation(
                    verticalSpacingHeight = verticalSpacingHeight,
                    screenWidth = screenWidth,
                    onGenerateEntrySelected = { onGenerateEntrySelected(selectedLanguage) },
                    onUserHasOwnPhrase = onUserHasOwnPhrase
                )
            } else {
                UserHasOwnPhrase(
                    verticalSpacingHeight = verticalSpacingHeight,
                    screenWidth = screenWidth,
                    onManualEntrySelected = { onManualEntrySelected(selectedLanguage) },
                    onPasteEntrySelected = onPasteEntrySelected,
                    onPictureSelected = {}//TODO: Setup callback
                )
            }
        }
    }
}

@Composable
fun ColumnScope.SelectPhraseCreation(
    verticalSpacingHeight: Dp, screenWidth: Dp,
    onGenerateEntrySelected: () -> Unit, onUserHasOwnPhrase: () -> Unit
) {
    Spacer(modifier = Modifier.height(verticalSpacingHeight))

    StandardButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = onGenerateEntrySelected,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = co.censo.shared.R.drawable.wand_and_stars),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(screenWidth * 0.020f))
            Text(
                text = stringResource(R.string.generate_seed_phrase),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }


    Spacer(modifier = Modifier.height(verticalSpacingHeight))

    StandardButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = onUserHasOwnPhrase,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = co.censo.shared.R.drawable.paste_phrase_icon),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(screenWidth * 0.020f))
            Text(
                text = stringResource(R.string.i_have_my_own),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }

    Spacer(modifier = Modifier.height(verticalSpacingHeight))
}

@Composable
fun UserHasOwnPhrase(
    verticalSpacingHeight: Dp, screenWidth: Dp,
    onManualEntrySelected: () -> Unit,
    onPasteEntrySelected: () -> Unit,
    onPictureSelected: () -> Unit
) {
    Spacer(modifier = Modifier.height(verticalSpacingHeight))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.manual_entry_icon),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue,
                modifier = Modifier
                    .padding(16.dp)
                    .drawBehind {
                        drawCircle(
                            color = SharedColors.ButtonBackgroundBlue,
                            radius = this.size.maxDimension
                        )
                    }
                    .clickable {
                        onManualEntrySelected()
                    }
            )
            Text(
                text = stringResource(R.string.input),
                color = SharedColors.MainColorText,
                fontSize = 20.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            //TODO: Need to fix this so that the size matches the other icons in the row
            // Easiest route is to get a drawable of the icon and use painter resource

            val cameraIcon = Icons.Outlined.CameraAlt
            Icon(
                imageVector = cameraIcon,
                contentDescription = "Camera",
                tint = SharedColors.ButtonTextBlue,
                modifier = Modifier
                    .padding(16.dp)
                    .drawBehind {
                        drawCircle(
                            color = SharedColors.ButtonBackgroundBlue,
                            radius = this.size.maxDimension
                        )
                    }
                    .clickable {
                        onPictureSelected()
                    }
            )
            Text(
                text = "Picture",
                color = SharedColors.MainColorText,
                fontSize = 20.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = co.censo.shared.R.drawable.paste_phrase_icon),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue,
                modifier = Modifier
                    .padding(16.dp)
                    .drawBehind {
                        drawCircle(
                            color = SharedColors.ButtonBackgroundBlue,
                            radius = this.size.maxDimension
                        )
                    }
                    .clickable {
                        onPasteEntrySelected()
                    }
            )
            Text(
                text = stringResource(R.string.paste),
                color = SharedColors.MainColorText,
                fontSize = 20.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(verticalSpacingHeight))
}

@Composable
fun NotInitialPhrase(
    verticalSpacingHeight: Dp, screenWidth: Dp,
    onManualEntrySelected: () -> Unit,
    onPasteEntrySelected: () -> Unit,
    onGenerateEntrySelected: () -> Unit
) {
    StandardButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = {
            onManualEntrySelected()
        },
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.manual_entry_icon),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(screenWidth * 0.010f))
            Text(
                text = stringResource(R.string.input_seed_phrase),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }

    Spacer(modifier = Modifier.height(verticalSpacingHeight))

    StandardButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = onPasteEntrySelected,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = co.censo.shared.R.drawable.paste_phrase_icon),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(screenWidth * 0.010f))
            Text(
                text = stringResource(R.string.paste_seed_phrase),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }

    Spacer(modifier = Modifier.height(verticalSpacingHeight))

    StandardButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = onGenerateEntrySelected,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = co.censo.shared.R.drawable.wand_and_stars),
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(screenWidth * 0.020f))
            Text(
                text = stringResource(R.string.generate_seed_phrase),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }

    Spacer(modifier = Modifier.height(verticalSpacingHeight))
}


@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePreviewEnterPhraseMainScreen() {
    SelectSeedPhraseEntryType(
        onManualEntrySelected = {},
        onPasteEntrySelected = {},
        onGenerateEntrySelected = {},
        welcomeFlow = true,
        savedPhrases = 0,
        currentLanguage = BIP39.WordListLanguage.English,
        userHasOwnPhrase = false,
        onUserHasOwnPhrase = {}
    )
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePreviewInitialPhraseScreen() {
    SelectSeedPhraseEntryType(
        onManualEntrySelected = {},
        onPasteEntrySelected = {},
        onGenerateEntrySelected = {},
        welcomeFlow = true,
        savedPhrases = 1,
        currentLanguage = BIP39.WordListLanguage.English,
        userHasOwnPhrase = false,
        onUserHasOwnPhrase = {}
    )
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun NormalPreviewEnterPhraseMainScreen() {
    SelectSeedPhraseEntryType(
        onManualEntrySelected = {},
        onPasteEntrySelected = {},
        savedPhrases = 2,
        onGenerateEntrySelected = {},
        welcomeFlow = false,
        currentLanguage = BIP39.WordListLanguage.English,
        userHasOwnPhrase = true,
        onUserHasOwnPhrase = {}
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallSeedPhraseAddedPreview() {
    SelectSeedPhraseEntryType(
        onManualEntrySelected = {},
        onPasteEntrySelected = {},
        onGenerateEntrySelected = {},
        savedPhrases = 2,
        welcomeFlow = false,
        currentLanguage = BIP39.WordListLanguage.English,
        userHasOwnPhrase = false,
        onUserHasOwnPhrase = {}
    )
}