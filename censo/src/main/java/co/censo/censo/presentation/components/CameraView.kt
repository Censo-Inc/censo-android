package co.censo.censo.presentation.components

import StandardButton
import TitleText
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import co.censo.censo.MainActivity
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.projectLog
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraView(
    executor: Executor,
    onImageCaptured: (ImageProxy) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    //region 1. Camera Setup
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                        )
                    )
                    .build()
            )
            .build()
    }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
    //endregion

    //region 2. LaunchedEffect to get camera provider and bind to lifecycle
    LaunchedEffect(key1 = lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    //endregion

    //region 3. Camera UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        //Title
        TitleText(
            title = stringResource(R.string.let_s_take_a_photo_of_your_seed_phrase),
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        )
        
        Spacer(modifier = Modifier.height(36.dp))

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        Box(modifier = Modifier
            .width(screenWidth)
            .height(screenWidth)
            .clipToBounds())
        {
            //Actual camera view here
            AndroidView({ previewView }, modifier = Modifier.matchParentSize())
        }

        Spacer(modifier = Modifier.height(36.dp))

        //Divider
        Divider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(),
            color = SharedColors.DividerGray
        )

        //Button
        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            onClick = {
                takePhotoAndReturnImageData(
                    imageCapture,
                    executor,
                    onImageCaptured,
                    onError
                )
            }) {
            Text(
                text = stringResource(id = R.string.take_a_photo),
                style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    //endregion
}

private fun takePhotoAndReturnImageData(
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (ImageProxy) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            projectLog(tag = MainActivity.prototypeTag, message = "Take photo success")
            onImageCaptured(image)
            //Close image when done
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            super.onError(exception)
            projectLog(tag = MainActivity.prototypeTag, message = "Take photo error: $exception")
            onError(exception)
        }
    })
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }


//TODO: Add a wrapper level composable for the view actions
@Composable
fun ImageReview(
    imageBitmap: ImageBitmap,
    imageContainerSizeFraction: Float = 0.85f,
    onSaveImage: (() -> Unit)?,
    onCancelImageSave: (() -> Unit)?,
    onDoneViewing: (() -> Unit)?
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TitleText(
            title = "Zoom in to review the words",
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        //Image Preview here
        val containerSize = LocalConfiguration.current.screenWidthDp.dp * imageContainerSizeFraction
        Box(
            modifier = Modifier
                .size(containerSize)
                .clipToBounds()
        ) {
            ZoomableImage(
                imageBitmap = imageBitmap,
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Divider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(),
            color = SharedColors.DividerGray
        )

        if (onSaveImage != null && onCancelImageSave != null) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onSaveImage()
                }) {
                Text(
                    text = "Use Photo",
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onCancelImageSave()
                }) {
                Text(
                    text = "Retake",
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }
        }

        if (onDoneViewing != null) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = {
                    onDoneViewing()
                }) {
                Text(
                    text = "Done",
                    style = ButtonTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ZoomableImage(
    imageBitmap: ImageBitmap,
    modifier: Modifier
) {

    val defaultScale = 1f
    val zoomedScale = 1.5f
    var scale by remember { mutableFloatStateOf(defaultScale) }
    //TODO: Offset should just affect the scrolling of the image
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformGestureModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale *= zoom
            offset += pan
        }
    }

    val doubleTapGestureModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
            scale = if (scale > defaultScale) defaultScale else zoomedScale
        })
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .fillMaxSize()
            .then(transformGestureModifier)
            .then(doubleTapGestureModifier)
    )
}


