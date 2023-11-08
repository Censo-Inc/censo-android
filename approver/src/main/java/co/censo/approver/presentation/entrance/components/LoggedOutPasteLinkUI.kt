package co.censo.approver.presentation.entrance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import co.censo.approver.presentation.components.PasteLink

@Composable
fun LoggedOutPasteLinkUI(
    onPasteLinkClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            PasteLink(
                onPasteLinkClick = onPasteLinkClick
            )

            Spacer(modifier = Modifier.weight(0.7f))
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoggedOutPasteLinkUIPreview() {
    LoggedOutPasteLinkUI(
        onPasteLinkClick = {}
    )
}
