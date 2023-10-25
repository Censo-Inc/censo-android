package co.censo.vault.presentation.access_seed_phrases.components

import LearnMore
import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R
import co.censo.vault.presentation.welcome.SetupStep

@Composable
fun ReadyToAccessPhrase(
    getStarted: () -> Unit
) {
    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.welcome_to_censo),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(all = 32.dp)
                .fillMaxWidth()
        )
        Text(
            stringResource(id = R.string.welcome_blurb),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(32.dp)
        ) {
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.google),
                heading = stringResource(R.string.authenticate_privately),
                content = stringResource(R.string.authenticate_privately_blurb),
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.small_face_scan),
                heading = stringResource(id = R.string.scan_your_face),
                content = stringResource(id = R.string.scan_your_face_blurb),
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
                heading = stringResource(id = R.string.enter_your_phrase),
                content = stringResource(id = R.string.enter_your_phrase_blurb),
            )
            Divider()
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.two_people),
                heading = stringResource(id = R.string.add_approvers),
                content = stringResource(id = R.string.add_approvers_blurb),
            )
            Divider()
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            onClick = getStarted,
            color = Color.Black
        ) {
            Text(
                text = stringResource(id = R.string.get_started),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                modifier = Modifier.padding(all = 8.dp)
            )
        }

        LearnMore {

        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewReadyToAccessPhrase() {
    ReadyToAccessPhrase {

    }
}