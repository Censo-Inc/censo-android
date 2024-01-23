package co.censo.shared.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

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
//endregion