package co.censo.censo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors

@Composable
fun SetupStep(
    imagePainter: Painter,
    heading: String,
    content: String,
    imageBackgroundColor : Color = SharedColors.BackgroundGrey,
    iconColor: Color = SharedColors.MainIconColor,
    completionText: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(color = imageBackgroundColor, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Icon(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp),
                tint = iconColor
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = heading,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SharedColors.MainColorText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 16.0.sp,
                color = SharedColors.MainColorText
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (completionText != null) {
                Text(
                    text = "✓ $completionText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedColors.SuccessGreen,
                )
            }
        }
    }
}
