package co.censo.censo.presentation.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.TotpCodeView

data class ShareTheCodeUIData(
    val heading: List<String>,
    val content: String,
    val verificationCodeUIData: VerificationCodeUIData? = null
)

@Composable
fun StepButton(icon: Painter, text: String, onClick: () -> Unit) {
    StandardButton(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = icon,
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }
}

@Composable
fun StepTitleText(heading: String) {
    Text(
        text = heading,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = SharedColors.MainColorText
    )
}

@Composable
fun StepContentText(content: String) {
    Text(
        text = content,
        fontSize = 18.sp,
        color = SharedColors.MainColorText
    )
}

data class VerificationCodeUIData(
    val code: String,
    val timeLeft: Int
)
@Composable
fun ApproverStep(
    heading: List<String>,
    content: String,
    buttonText: String = stringResource(id = R.string.share),
    verificationCodeUIData: VerificationCodeUIData? = null,
    onClick: (() -> Unit)? = null,
    includeLine: Boolean = true,
    imageContent: @Composable() ((ColumnScope.() -> Unit))
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageContent()
            if (includeLine) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(color = SharedColors.ButtonBackgroundBlue)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
        ) {
            heading.forEach { line ->
                StepTitleText(heading = line)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StepContentText(content = content)
            Spacer(modifier = Modifier.height(8.dp))
            onClick?.let {
                StepButton(
                    icon = painterResource(id = co.censo.shared.R.drawable.share_link),
                    text = buttonText,
                    onClick = it
                )
            }
            verificationCodeUIData?.let {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    TotpCodeView(
                        code = it.code,
                        secondsLeft = it.timeLeft,
                        primaryColor = SharedColors.MainColorText
                    )
                }
            }
        }
    }
}