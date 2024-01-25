package co.censo.shared.util

import android.content.Context
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
import co.censo.shared.R
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

fun getImageCaptureErrorDisplayMessage(exception: Exception?, context: Context) : String {
    if (exception == null || exception !is ImageCaptureException) {
        return context.getString(R.string.unable_to_capture_photo_error)
    }

    return getImageCaptureErrorMessage(exception, context)
}

fun getImageCaptureErrorMessage(exception: ImageCaptureException, context: Context): String {
    return when (exception.imageCaptureError ) {

        ERROR_INVALID_CAMERA -> {
            context.getString(R.string.invalid_camera_error)
        }

        ERROR_CAMERA_CLOSED -> {
            context.getString(R.string.camera_closed_error)
        }

        ERROR_CAPTURE_FAILED -> {
            context.getString(R.string.failed_to_capture_photo_error)
        }

        ERROR_FILE_IO,
        ERROR_UNKNOWN,
        -> {
            context.getString(R.string.unable_to_capture_photo_error)
        }

        else -> {
            context.getString(R.string.unable_to_capture_photo_error)
        }
    }
}
//endregion