package co.censo.censo.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import co.censo.censo.MainActivity
import co.censo.censo.MainActivity.Companion.prototypeTag
import co.censo.shared.util.projectLog
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min

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
    val previewView  = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
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
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView({ previewView}, modifier = Modifier.fillMaxSize())

        IconButton(
            modifier = Modifier.padding(bottom = 20.dp) ,
            onClick = {
                projectLog(tag = prototypeTag, message = "ON IMAGE CAPTURE")
                takePhotoAndReturnImageData(imageCapture, executor, onImageCaptured, onError)
            }
        ) {
            Icon(
                imageVector = Icons.Sharp.Lens,
                contentDescription = "Take picture",
                tint = Color.White,
                modifier = Modifier
                    .size(100.dp)
                    .padding(1.dp)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
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

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

//region Camera utils
fun imageProxyToByteArray(image: ImageProxy) : ByteArray {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

fun rotateBitmap(source: Bitmap, angle: Float) : Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
//endregion

@Composable
fun ImagePreview(
    imageBitmap: ImageBitmap,
    imageContainerSizeFraction: Float = 0.75f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        val containerSize = LocalConfiguration.current.screenWidthDp.dp * imageContainerSizeFraction
        Box(
            modifier = Modifier
                .size(containerSize)
                .clipToBounds()
                .align(Alignment.Center)
        ) {
            ZoomableImage(
                imageBitmap = imageBitmap,
                modifier = Modifier

            )
        }
    }
}

@Composable
fun ZoomableImage(
    imageBitmap: ImageBitmap,
    modifier: Modifier
) {

    //TODO: Need to re-introduce the offset parameter here to move the image around once zoomed

    val defaultScale = 1f
    val zoomedScale = 1.5f
    var scale by remember { mutableFloatStateOf(defaultScale) }

    val transformGestureModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale *= zoom
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
            }
            .fillMaxSize()
            .then(transformGestureModifier)
            .then(doubleTapGestureModifier)
    )
}


