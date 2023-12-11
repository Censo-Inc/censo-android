package co.censo.censo.presentation.main

import StandardButton
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle

@Composable
fun VaultHomeScreen(
    seedPhrasesSaved: Int,
    onAddSeedPhrase: () -> Unit,
    onAddApprovers: () -> Unit,
    showAddApprovers: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildSeedPhraseCount(seedPhrasesSaved, context),
            fontSize = 28.sp,
            fontWeight = FontWeight.W700,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildAddSeedPhraseDisclaimer(
                context = context,
                multiplePhrases = seedPhrasesSaved > 1
            ),
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            onClick = onAddSeedPhrase
        ) {
            Text(
                text = stringResource(id = R.string.add_seed_phrase),
                style = ButtonTextStyle.copy(fontWeight = FontWeight.W400)
            )
        }

        if (showAddApprovers) {
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.you_can_increase_security_by_adding_approvers),
                fontSize = 26.sp,
                fontWeight = FontWeight.W700,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onAddApprovers
            ) {
                Text(
                    text = stringResource(id = R.string.add_approvers_button_text),
                    style = ButtonTextStyle.copy(fontWeight = FontWeight.W400)
                )
            }
        }
    }
}

private fun buildSeedPhraseCount(count: Int, context: Context) : AnnotatedString {
    val countSpanStyle = SpanStyle(
        fontSize = 38.sp,
        fontWeight = FontWeight.W700
    )

    val textSpanStyle = SpanStyle(
        fontSize = 26.sp,
    )

    return buildAnnotatedString {
        withStyle(textSpanStyle) {
            append(context.getString(R.string.you_have_span))
        }
        withStyle(countSpanStyle) {
            append(count.toString())
        }
        withStyle(textSpanStyle) {
            if (count > 1) {
                append(context.getString(R.string.seed_phrases_span))
            } else {
                append(context.getString(R.string.seed_phrase_span))
            }
        }
    }
}

private fun buildAddSeedPhraseDisclaimer(context: Context, multiplePhrases: Boolean) : AnnotatedString {
    val emphasisSpanStyle = SpanStyle(
        fontWeight = FontWeight.W600
    )

    return buildAnnotatedString {
        if (multiplePhrases) {
            append(context.getString(R.string.they_are_stored_securely))
        } else {
            append(context.getString(R.string.it_is_stored_securely_and_accessible_span))
        }

        append(" ")

        withStyle(emphasisSpanStyle) {
            append(context.getString(R.string.only_span))
        }

        append(context.getString(R.string.to_you_span))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun VaultHomePreview() {
    VaultHomeScreen(
        onAddApprovers = {},
        onAddSeedPhrase = {},
        showAddApprovers = true,
        seedPhrasesSaved = 2,
    )
}