package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.plan_setup.components.ActionButtonUIData
import co.censo.censo.presentation.plan_setup.components.VerificationCodeUIData
import co.censo.shared.presentation.components.TotpCodeView

@Composable
fun PastePhraseUI(
    onPasteLink: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Spacer(modifier = Modifier.height(32.dp))
            TitleText(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                title = R.string.paste_your_phrase
            )
            Spacer(modifier = Modifier.height(56.dp))
            PastePhraseStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.paste_phrase_icon),
                heading = stringResource(R.string.copy_your_seed_phrase),
                content = stringResource(R.string.copy_seed_phrase_message)
            )
            Spacer(modifier = Modifier.height(24.dp))
            PastePhraseStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
                heading = stringResource(R.string.click_the_button_below),
                content = stringResource(R.string.click_past_button_message)
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onPasteLink,
        ) {
            Text(
                text = stringResource(R.string.paste_from_clipboard),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun PastePhraseStep(
    imagePainter: Painter,
    heading: String,
    content: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(
                    color = SharedColors.WordBoxBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp),
                colorFilter = ColorFilter.tint(color = Color.Black)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = heading, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = content, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPastePhraseUI() {
    PastePhraseUI {

    }
}