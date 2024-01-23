package co.censo.shared.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.ERROR_FILE_IO
import androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA
import androidx.camera.core.ImageCapture.ERROR_UNKNOWN
import androidx.camera.core.ImageCapture.ImageCaptureError
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.lang.Exception

//region Camera utils
fun Bitmap.bitmapToByteArray() : ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
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
    return when (exception.imageCaptureError) {

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
        ERROR_UNKNOWN,
        else -> {
            "Unable to capture photo"
        }
    }
}
//endregion