package co.censo.censo.presentation.components

import StandardButton
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.access_seed_phrases.components.TimeLeftForAccess
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import kotlin.time.Duration

@Composable
fun ImageReview(
    imageBitmap: ImageBitmap,
    imageContainerSizeFraction: Float = 0.85f,
    timeLeft: Duration? = null,
    isAccessReview: Boolean,
    onSaveImage: (() -> Unit)?,
    onCancelImageSave: (() -> Unit)?,
    onDoneViewing: (() -> Unit)?
) {
    val componentPadding = PaddingValues(horizontal = 16.dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        timeLeft?.let {
            TimeLeftForAccess(timeLeft = timeLeft)
        }

        Spacer(modifier = Modifier.weight(0.5f))

        val titleMessage = if (isAccessReview) stringResource(R.string.zoom_in_to_see_the_words) else  stringResource(
            R.string.zoom_in_to_review_the_words)
        TitleText(
            title = titleMessage,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.weight(0.35f))

        //Image Preview here
        val containerSize = LocalConfiguration.current.screenWidthDp.dp * imageContainerSizeFraction
        Box(
            modifier = Modifier
                .size(containerSize)
                .padding(componentPadding)
                .clipToBounds()
        ) {
            ZoomableImage(
                imageBitmap = imageBitmap,
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .padding(componentPadding),
            color = SharedColors.DividerGray
        )

        Spacer(modifier = Modifier.weight(1f))

        if (onSaveImage != null && onCancelImageSave != null) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onSaveImage()
                }) {
                Text(
                    text = stringResource(R.string.use_photo),
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onCancelImageSave()
                }) {
                Text(
                    text = stringResource(R.string.retake),
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }
        }

        if (onDoneViewing != null) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onDoneViewing()
                }) {
                Text(
                    text = stringResource(R.string.done),
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ZoomableImage(
    imageBitmap: ImageBitmap,
    modifier: Modifier
) {

    val defaultScale = 1f
    val zoomedScale = 3f
    var scale by remember { mutableFloatStateOf(defaultScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val transformGestureModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            val oldScale = scale
            scale *= zoom
            scale = scale.coerceIn(defaultScale, zoomedScale)

            //Adjust offset
            val newOffsetX = (offset.x + pan.x) * (scale / oldScale)
            val newOffsetY = (offset.y + pan.y) * (scale / oldScale)

            //Calculate boundaries
            val maxX = (imageSize.width * scale - imageSize.width) / 2f
            val maxY = (imageSize.height * scale - imageSize.height) / 2f

            offset = Offset(
                x = newOffsetX.coerceIn(-maxX, maxX),
                y = newOffsetY.coerceIn(-maxY, maxY)
            )
        }
    }

    val doubleTapGestureModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
            scale = if (scale > defaultScale) defaultScale else zoomedScale
            offset = Offset.Zero
        })
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = modifier
            .onSizeChanged {
                imageSize = it
            }
            .then(transformGestureModifier)
            .then(doubleTapGestureModifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}


