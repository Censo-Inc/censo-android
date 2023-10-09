package co.censo.vault.presentation.components.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecoveryApprovalsCollected(
    collected: Int,
    required: Int
) {
    Row(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,

        ) {

        Text(
            text = "$collected",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.W400,
        )

        Spacer(modifier = Modifier.size(20.dp))

        Text(
            modifier = Modifier.padding(bottom = 6.dp),
            text = "of",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.W400,
        )

        Spacer(modifier = Modifier.size(20.dp))

        Text(
            text = "$required",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.W400,
        )
    }
}


@Preview
@Composable
fun RecoveryApprovalsCollectedPreview() {
    RecoveryApprovalsCollected(
        collected = 1,
        required = 3
    )
}