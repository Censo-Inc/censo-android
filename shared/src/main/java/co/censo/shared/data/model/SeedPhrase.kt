package co.censo.shared.data.model

import co.censo.shared.data.model.SeedPhraseData.Companion.imageTypeCode
import co.censo.shared.data.model.SeedPhraseData.Companion.phraseHeaderByte
import co.censo.shared.util.BIP39
sealed class SeedPhraseData {
    companion object {
        const val phraseHeaderByte: UInt = 255u
        const val imageTypeCode: UInt = 1u
    }

    data class Image(val imageData: ByteArray) : SeedPhraseData()

    data class Bip39(val words: List<String>) : SeedPhraseData()
}

fun SeedPhraseData.toData() : ByteArray {
    return when (this) {
        is SeedPhraseData.Image -> (byteArrayOf(
            phraseHeaderByte.toByte(),
            imageTypeCode.toByte()
        )) + this.imageData

        is SeedPhraseData.Bip39 -> BIP39.wordsToBinaryData(this.words)
    }
}

fun SeedPhraseData.fromData(data: ByteArray, language: BIP39.WordListLanguage? = null) : SeedPhraseData {
    if (data.count() < 2) {
        throw Exception("Invalid phrase data")
    }

    when (data[0]) {
        phraseHeaderByte.toByte() -> {
            when (data[1]) {
                imageTypeCode.toByte() -> {
                    return SeedPhraseData.Image(imageData = data.drop(2).toByteArray())
                }

                else -> {
                    throw Exception("Invalid phrase data")
                }
            }
        }

        else -> {
            return SeedPhraseData.Bip39(words = BIP39.binaryDataToWords(binaryData = data, language = language))
        }
    }
}