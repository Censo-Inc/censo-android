package co.censo.shared.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.ERROR_FILE_IO
import androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA
import androidx.camera.core.ImageCapture.ERROR_UNKNOWN
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import java.io.ByteArrayOutputStream
import kotlin.Exception

//region Camera utils

fun buildImageCapture(): ImageCapture {
    val resolutionStrategy =
        ResolutionStrategy(
            Size(1280, 720),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
        )

    return ImageCapture.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .build()
        )
        .build()
}

fun buildCameraSelector(lensFacing: Int) : CameraSelector {
    return CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
}

fun Bitmap?.bitmapToByteArray() : ByteArray? {
    if (this == null) {
        return null
    }

    return try {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.toByteArray()
    } catch (e: Exception) {
        e.sendError(CrashReportingUtil.ImageCapture)
        null
    }
}

fun ByteArray.byteArrayToBitmap() : Bitmap? {



    return BitmapFactory.decodeByteArray(this, 0, this.size)
}

fun rotateBitmap(source: Bitmap, angle: Float) : Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

fun getImageCaptureErrorDisplayMessage(exception: Exception?) : String {
    if (exception == null || exception !is ImageCaptureException) {
        return "Unable to capture photo"
    }

    return getImageCaptureErrorMessage(exception)
}

fun getImageCaptureErrorMessage(exception: ImageCaptureException): String {
    return when (exception.imageCaptureError ) {

        ERROR_INVALID_CAMERA -> {
            "Invalid camera"
        }

        ERROR_CAMERA_CLOSED -> {
            "Camera closed"
        }

        ERROR_CAPTURE_FAILED -> {
            "Failed to capture photo"
        }

        ERROR_FILE_IO,
        ERROR_UNKNOWN, -> {
            "Unable to capture photo"
        }

        else -> {
            "Unable to capture photo"
        }
    }
}
//endregion