package co.censo.censo.presentation.main

import StandardButton
import SeedPhraseId
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.PhraseType
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

@Composable
fun PhraseHomeScreen(
    seedPhrases: List<SeedPhrase>,
    onAddClick: () -> Unit,
    onAccessClick: () -> Unit,
    onPhraseNotesClick: (SeedPhrase) -> Unit,
    onRenamePhraseClick: (SeedPhrase) -> Unit,
    onDeletePhraseClick: (SeedPhrase) -> Unit,
    onCancelAccessClick: () -> Unit,
    accessButtonLabel: AccessButtonLabelEnum,
    timelockExpiration: Instant?,
    accessButtonEnabled: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

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
                        isEditable = true,
                        onNotes = { onPhraseNotesClick(seedPhrase) },
                        onRename = { onRenamePhraseClick(seedPhrase) },
                        onDelete = { onDeletePhraseClick(seedPhrase) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Column(
            Modifier
                .background(color = Color.White)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Divider(
                modifier = Modifier
                    .height(1.5.dp)
                    .fillMaxWidth(),
                color = SharedColors.DividerGray
            )

            if (timelockExpiration != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(id = R.drawable.time_left_icon),
                        contentDescription = "",
                        tint = SharedColors.MainIconColor
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                    val accessTextStyle = SpanStyle(
                        fontSize = 16.sp,
                        color = SharedColors.MainColorText
                    )

                    val timeLeftText = buildAnnotatedString {
                        withStyle(accessTextStyle) {
                            append(stringResource(R.string.timelock_expires_in))
                        }
                        withStyle(accessTextStyle.copy(fontWeight = FontWeight.W600)) {
                            append(
                                formatTimelockDuration(
                                    timelockExpiration - Clock.System.now(),
                                    context
                                )
                            )
                        }
                    }

                    Text(
                        text = timeLeftText,
                        color = SharedColors.MainColorText
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StandardButton(
                    modifier = Modifier.weight(0.5f),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    onClick = {
                        if (accessButtonLabel == AccessButtonLabelEnum.CancelAccess) {
                            onCancelAccessClick()
                        } else {
                            onAccessClick()
                        }
                    },
                    enabled = accessButtonEnabled
                ) {
                    val buttonTextStyle =
                        if (accessButtonEnabled) ButtonTextStyle else DisabledButtonTextStyle
                    Text(
                        text = stringResource(when (accessButtonLabel) {
                            AccessButtonLabelEnum.BeginAccess -> R.string.begin_access
                            AccessButtonLabelEnum.RequestAccess -> R.string.request_access
                            AccessButtonLabelEnum.CancelAccess -> R.string.cancel_access
                            AccessButtonLabelEnum.ShowSeedPhrases -> R.string.show_seed_phrases
                        }),
                        style = buttonTextStyle.copy(fontSize = 20.sp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StandardButton(
                    modifier = Modifier.weight(0.5f),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    onClick = onAddClick
                ) {
                    Text(
                        text = stringResource(R.string.add_seed_phrase),
                        style = ButtonTextStyle.copy(fontSize = 20.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeedPhraseItem(
    seedPhrase: SeedPhrase,
    isEditable: Boolean = false,
    isSelected: Boolean = false,
    hasNotes: Boolean = false,
    onClick: (() -> Unit)? = null,
    onNotes: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val modifier = onClick?.let {
        if (!isEditable) {
            Modifier.clickable { it() }
        } else Modifier
    } ?: Modifier
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) SharedColors.SuccessGreen else SharedColors.BorderGrey,
                    shape = RoundedCornerShape(12.dp)
                )
                .height(120.dp)
                .weight(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                modifier = Modifier
                    .height(42.dp)
                    .padding(horizontal = 16.dp)
                    .weight(.45f),
                painter = painterResource(id = when (seedPhrase.type) {
                    PhraseType.Binary -> R.drawable.textbox
                    PhraseType.Photo -> R.drawable.photo
                }),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(if (isSelected) SharedColors.SuccessGreen else SharedColors.MainColorText)
            )
            Text(
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp,
                        start = 8.dp,
                        end = if (isEditable) 0.dp else 16.dp
                    )
                    .weight(1f),
                text = seedPhrase.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.W600,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                color = if (isSelected) SharedColors.SuccessGreen else SharedColors.MainColorText
            )

            if (isEditable) {
                Box(
                    modifier = Modifier
                        .background(color = Color.White)
                        .padding(end = 8.dp)
                        .weight(0.25f)
                        .wrapContentSize(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .size(36.dp),
                        contentDescription = stringResource(R.string.edit_phrase),
                        tint = SharedColors.MainIconColor
                    )
                    DropdownMenu(
                        modifier = Modifier.background(color = Color.White),
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
                            text = { Text(stringResource(R.string.notes), fontSize = 20.sp, color = Color.Black) },
                            onClick = {
                                onNotes?.invoke()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
                            text = { Text(stringResource(id = R.string.rename), fontSize = 20.sp, color = Color.Black) },
                            onClick = {
                                onRename?.invoke()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
                            text = { Text(stringResource(id = R.string.delete), fontSize = 20.sp, color = Color.Red) },
                            onClick = {
                                onDelete?.invoke()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (!isEditable && !isSelected) {
            Icon(
                modifier = Modifier
                    .clickable { onClick?.invoke() }
                    .size(48.dp)
                    .weight(0.25f)
                    .align(Alignment.CenterVertically),
                imageVector = when (hasNotes) {
                    true -> Icons.Filled.ChevronRight
                    false -> Icons.Filled.Add
                },
                contentDescription = stringResource(R.string.select_phrase_cont_description),
                tint = SharedColors.MainIconColor
            )
        }

        if (isSelected) {
            Icon(
                painterResource(id = co.censo.shared.R.drawable.check_icon),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(48.dp)
                    .weight(0.25f)
                    .padding(horizontal = 10.dp),
                contentDescription = stringResource(R.string.phrase_viewed),
                tint = SharedColors.SuccessGreen
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseHomeScreenWithTimelock() {
    PhraseHomeScreen(
        onAccessClick = {},
        onAddClick = {},
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("3"),
                label = "SEED PHRASE WITH A VERY LONG NAME OF 50 CHARACTERS",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
        ),
        onPhraseNotesClick = {},
        onRenamePhraseClick = {},
        onDeletePhraseClick = {},
        onCancelAccessClick = {},
        accessButtonLabel = AccessButtonLabelEnum.RequestAccess,
        timelockExpiration = Clock.System.now() + 5.minutes,
        accessButtonEnabled = true
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseHomeScreenNoTimelock() {
    PhraseHomeScreen(
        onAccessClick = {},
        onAddClick = {},
        seedPhrases = listOf(
            SeedPhrase(
                guid = SeedPhraseId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Photo,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("3"),
                label = "SEED PHRASE WITH A VERY LONG NAME OF 50 CHARACTERS",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Photo,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("4"),
                label = "yet another phrase",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
            SeedPhrase(
                guid = SeedPhraseId("5"),
                label = "and another phrase",
                seedPhraseHash = HashedValue(""),
                type = PhraseType.Binary,
                createdAt = Clock.System.now(),
                encryptedNotes = null,
            ),
        ),
        onPhraseNotesClick = {},
        onRenamePhraseClick = {},
        onDeletePhraseClick = {},
        onCancelAccessClick = {},
        accessButtonLabel = AccessButtonLabelEnum.RequestAccess,
        timelockExpiration = null,
        accessButtonEnabled = true
    )
}