package co.censo.shared.presentation.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ColumnScope.NeedsToSaveKeyUI(
    message: String,
    onSaveKey: () -> Unit,
) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = message,
        textAlign = TextAlign.Center,
        color = SharedColors.MainColorText,
        fontSize = 20.sp
    )
    Spacer(modifier = Modifier.height(24.dp))
    StandardButton(
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp),
        onClick = onSaveKey
    ) {
        Text(
            text = stringResource(R.string.save_key_to_cloud),
            style = ButtonTextStyle
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewNeedsToSaveKey() {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NeedsToSaveKeyUI(
            message = "To use this app you need to save your key."
        ) {

        }
    }
}