package co.censo.shared.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.censo.shared.presentation.SharedColors

@Composable
fun SmallLoading(
    color: Color = ProgressIndicatorDefaults.circularColor,
    fullscreen: Boolean,
    fullscreenBackgroundColor: Color? = null
) {
    Loading(
        color = color,
        fullscreen = fullscreen,
        fullscreenBackgroundColor = fullscreenBackgroundColor,
        size = 24.dp,
        strokeWidth = 3.5.dp
    )
}

@Composable
fun LargeLoading(
    color: Color = SharedColors.DefaultLoadingColor,
    fullscreen: Boolean,
    fullscreenBackgroundColor: Color? = null
) {
    Loading(
        color = color,
        fullscreen = fullscreen,
        fullscreenBackgroundColor = fullscreenBackgroundColor,
        size = 72.dp,
        strokeWidth = 8.dp
    )
}

@Composable
fun Loading(
    strokeWidth: Dp,
    color: Color = SharedColors.DefaultLoadingColor,
    size: Dp,
    fullscreen: Boolean,
    fullscreenBackgroundColor: Color? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "inf_transition")
    val animatedAlpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.25f at 0 with LinearEasing
                1f at 500 with LinearEasing
                0.25f at 1000 with LinearEasing
            },
            repeatMode = RepeatMode.Reverse
        ), label = "anim_alpha"
    )

    val modifier = fullscreenBackgroundColor?.let {
        Modifier
            .fillMaxSize()
            .background(color = it)
    } ?: Modifier.fillMaxSize()


    if (fullscreen) {
        Box(
            modifier = modifier
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .alpha(animatedAlpha.value),
                strokeWidth = strokeWidth,
                color = color
            )
        }
    } else {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .alpha(animatedAlpha.value),
            strokeWidth = strokeWidth,
            color = color
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoadingFullscreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Loading(
            strokeWidth = 5.dp,
            color = Color.Black,
            size = 72.dp,
            fullscreen = true,
            fullscreenBackgroundColor = Color.White
        )
    }
}

@Preview(showBackground = false, showSystemUi = true)
@Composable
fun LoadingPreview() {
    Loading(
        strokeWidth = 5.dp,
        color = Color.White,
        size = 72.dp,
        fullscreen = false,
        fullscreenBackgroundColor = Color.Red
    )
}