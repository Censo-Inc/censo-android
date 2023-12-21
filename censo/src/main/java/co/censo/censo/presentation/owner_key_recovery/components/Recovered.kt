package co.censo.censo.presentation.owner_key_recovery.components

import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.SharedColors

@Composable
fun Recovered() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.confetti),
            contentDescription = null,
            contentScale = ContentScale.None,
        )

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.you_are_all_set),
            color = SharedColors.MainColorText,
            fontSize = 44.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecoveredPreview() {
    Recovered()
}