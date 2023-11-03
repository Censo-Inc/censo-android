package co.censo.censo.presentation.main

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R

@Composable
fun VaultHomeScreen(
    seedPhrasesSaved: Int,
    approvers: Int,
    onAddSeedPhrase: () -> Unit,
    onAddApprovers: () -> Unit,
    showAddApprovers: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VaultHomeContainer(
                value = seedPhrasesSaved,
                title = stringResource(R.string.seed_phrase_home_title),
                buttonText = stringResource(R.string.add_seed_phrase),
                onButtonClick = onAddSeedPhrase,
            )
        }

        Divider(
            modifier = Modifier.height(1.dp),
            color = SharedColors.DividerGray
        )

        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VaultHomeContainer(
                value = approvers,
                title = stringResource(R.string.approvers_home_title),
                buttonText = stringResource(R.string.add_approvers_button_text),
                dashedBorder = true,
                showButton = showAddApprovers,
                onButtonClick = onAddApprovers,
            )
        }

        Spacer(modifier = Modifier.height(44.dp))
    }
}

@Composable
fun VaultHomeContainer(
    value: Int,
    title: String,
    buttonText: String,
    dashedBorder: Boolean = false,
    showButton: Boolean = true,
    onButtonClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Spacer(modifier = Modifier.weight(0.1f))

        val modifier = if (dashedBorder) {
            Modifier
                .background(color = Color.White, shape = RoundedCornerShape(16.dp))
                .weight(0.8f)
                .dashedBorder(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    radius = 16.dp
                )
        } else {
            Modifier
                .background(color = Color.White, shape = RoundedCornerShape(16.dp))
                .weight(0.8f)
                .border(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    shape = RoundedCornerShape(16.dp)
                )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.05f))

        if (showButton) {
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                color = SharedColors.ButtonGrey,
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onButtonClick
            ) {
                Text(
                    text = buttonText,
                    color = Color.Black,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))
        } else {
            Spacer(modifier = Modifier.weight(0.20f))
        }
    }
}

fun Modifier.dashedBorder(width: Dp, radius: Dp, color: Color) =
    drawBehind {
        drawIntoCanvas {
            val paint = Paint()
                .apply {
                    strokeWidth = width.toPx()
                    this.color = color
                    style = PaintingStyle.Stroke
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                radius.toPx(),
                radius.toPx(),
                paint
            )
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
        approvers = 1
    )
}