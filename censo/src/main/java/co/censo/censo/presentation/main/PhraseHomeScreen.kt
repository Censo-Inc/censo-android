package co.censo.censo.presentation.main

import Base64EncodedData
import StandardButton
import VaultSecretId
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock

@Composable
fun PhraseHomeScreen(
    vaultSecrets: List<VaultSecret>,
    onAddClick: () -> Unit,
    onAccessClick: () -> Unit,
    onEditPhraseClick: (VaultSecret) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        AddOrAccessRow(onAddClick = onAddClick, onAccessClick = onAccessClick)
        Divider(
            modifier = Modifier
                .height(1.5.dp)
                .fillMaxWidth(),
            color = SharedColors.DividerGray
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            vaultSecrets.forEach { vaultSecret ->
                Spacer(modifier = Modifier.height(12.dp))
                SeedPhraseItem(
                    vaultSecret = vaultSecret,
                    isDeletable = true,
                    onClick = {
                        onEditPhraseClick(vaultSecret)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AddOrAccessRow(
    onAddClick: () -> Unit,
    onAccessClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SharedColors.BackgroundGrey.copy(alpha = 0.25f))
            .padding(horizontal = 24.dp),
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
fun SeedPhraseItem(
    vaultSecret: VaultSecret,
    isDeletable: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val modifier = onClick?.let {
        if (!isDeletable) {
            Modifier.clickable { it() }
        } else Modifier
    } ?: Modifier

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
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .padding(
                        top = 18.dp,
                        bottom = 18.dp,
                        start = 18.dp,
                        end = if (isDeletable) 0.dp else 18.dp
                    )
                    .weight(1f),
                text = vaultSecret.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.W600,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                color = if (isSelected) SharedColors.SuccessGreen else Color.Black
            )

            if (isDeletable) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.edit_icon),
                    modifier = Modifier
                        .clickable { onClick?.invoke() }
                        .size(36.dp)
                        .padding(end = 6.dp)
                        .weight(0.25f),
                    contentDescription = stringResource(R.string.edit_phrase),
                    tint = Color.Black
                )
            }
        }

        if (!isDeletable && !isSelected) {
            Icon(
                modifier = Modifier
                    .clickable { onClick?.invoke() }
                    .size(48.dp)
                    .weight(0.25f)
                    .align(Alignment.CenterVertically),
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.select_phrase_cont_description)
            )
        }

        if (isSelected) {
            Icon(
                painterResource(id = co.censo.shared.R.drawable.check_icon),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(48.dp)
                    .weight(0.25f)
                    .padding(horizontal = 24.dp),
                contentDescription = stringResource(R.string.phrase_viewed),
                tint = SharedColors.SuccessGreen
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseHomeScreen() {
    PhraseHomeScreen(
        onAccessClick = {},
        onAddClick = {},
        vaultSecrets = listOf(
            VaultSecret(
                guid = VaultSecretId("1"),
                label = "Yankee Hotel Foxtrot",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
            VaultSecret(
                guid = VaultSecretId("2"),
                label = "Robin Hood",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
            VaultSecret(
                guid = VaultSecretId("3"),
                label = "SEED PHRASE WITH A VERY LONG NAME OF 50 CHARACTERS",
                seedPhraseHash = HashedValue(""),
                encryptedSeedPhrase = Base64EncodedData(""),
                createdAt = Clock.System.now()
            ),
        ),
        onEditPhraseClick = {

        }
    )
}