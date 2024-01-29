package co.censo.shared.data.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import co.censo.shared.data.model.SeedPhraseData.Companion.imageTypeCode
import co.censo.shared.data.model.SeedPhraseData.Companion.phraseHeaderByte
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil.ImageReview
import co.censo.shared.util.byteArrayToBitmap
import co.censo.shared.util.sendError

sealed class SeedPhraseData {
    companion object {
        const val phraseHeaderByte: UInt = 255u
        const val imageTypeCode: UInt = 1u
    }

    data class Image(val imageData: ByteArray) : SeedPhraseData()

    data class Bip39(val words: List<String>) : SeedPhraseData()
}

fun SeedPhraseData.Image.getImageBitmap() : ImageBitmap? {
    return try {
        this.imageData.byteArrayToBitmap()?.asImageBitmap()
    } catch (e: Exception) {
        e.sendError(ImageReview)
        null
    }
}

fun SeedPhraseData.toByteArray() : ByteArray {
    return when (this) {
        is SeedPhraseData.Image -> (byteArrayOf(
            phraseHeaderByte.toByte(),
            imageTypeCode.toByte()
        )) + this.imageData

        is SeedPhraseData.Bip39 -> BIP39.wordsToBinaryData(this.words)
    }
}

fun ByteArray.toSeedPhraseData(language: BIP39.WordListLanguage? = null) : SeedPhraseData {
    if (this.count() < 2) {
        throw Exception("Invalid phrase data")
    }

    when (this[0]) {
        phraseHeaderByte.toByte() -> {
            when (this[1]) {
                imageTypeCode.toByte() -> {
                    return SeedPhraseData.Image(imageData = this.drop(2).toByteArray())
                }

                else -> {
                    throw Exception("Invalid phrase data")
                }
            }
        }

        else -> {
            return SeedPhraseData.Bip39(words = BIP39.binaryDataToWords(binaryData = this, language = language))
        }
    }
}