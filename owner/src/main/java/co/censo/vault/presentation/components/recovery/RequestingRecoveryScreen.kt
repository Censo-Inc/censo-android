package co.censo.vault.presentation.components.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.Colors

@Composable
fun RequestingRecoveryScreen() {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Colors.PrimaryBlue),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Initiating recovery",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.W400
        )

        Spacer(modifier = Modifier.height(18.dp))

        CircularProgressIndicator(
            modifier = Modifier
                .size(72.dp)
                .align(alignment = Alignment.CenterHorizontally),
            strokeWidth = 8.dp,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
fun RequestingRecoveryScreenPreview() {
    RequestingRecoveryScreen()
}