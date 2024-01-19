package co.censo.censo.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import co.censo.censo.MainActivity
import co.censo.censo.MainActivity.Companion.prototypeTag
import co.censo.shared.util.projectLog
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
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

private fun takePhotoAndSaveToFileStorage(
    filenameFormat: String,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(outputOptions, executor, object: ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            onImageCaptured(savedUri)
        }

        override fun onError(exception: ImageCaptureException) {
            projectLog(tag = prototypeTag, message = "Take photo error: $exception")
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

//Camera utils
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
