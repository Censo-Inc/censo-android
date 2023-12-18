package co.censo.censo.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.BIP39

@Composable
fun LanguageSelectionMenu(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text:  AnnotatedString,
    currentLanguage: BIP39.WordListLanguage?,
    action: (BIP39.WordListLanguage) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ClickableText(
            text = text,
            onClick = { _ ->
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 120.dp, max = 240.dp)
        ) {
            enumValues<BIP39.WordListLanguage>().toList().sortedBy { it.displayName() }.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        action(item)
                    },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${item.localizedDisplayName()}\n${item.displayName()}",
                                fontSize = 18.sp,
                                fontWeight = if (currentLanguage == item) FontWeight.Bold else FontWeight.Light,
                                textAlign = TextAlign.Left,
                                color = SharedColors.MainColorText
                            )
                        }
                    }
                )
                Divider()
            }
        }
    }

}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LanguageSelectionMenuPreview() {
    LanguageSelectionMenu(
        text = buildAnnotatedString { append("this is clickable text") },
        currentLanguage = BIP39.WordListLanguage.English,
        action = {}
    )
}