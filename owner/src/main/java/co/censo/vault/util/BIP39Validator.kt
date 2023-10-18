package co.censo.vault.util

import android.content.Context
import co.censo.shared.data.cryptography.toByteArrayNoSign
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.experimental.and

private val json = Json {
    ignoreUnknownKeys = true
}

sealed class BIP39Validation {
    object TooShort: BIP39Validation()
    object TooLong: BIP39Validation()

    object BadLength: BIP39Validation()

    data class InvalidWords(val wordsByIndex: Map<Int, String>): BIP39Validation()

    object InvalidChecksum: BIP39Validation()
}

object BIP39Validator {
    private lateinit var appContext: Context

    fun setup(context: Context) {
        appContext = context
    }
    private inline fun <reified R : Any> String.convertToDataClass() =
        json.decodeFromString<R>(this)

    val wordlist by lazy {
        appContext.assets.open("bip39.json").bufferedReader().use { it.readText() }.convertToDataClass<List<String>>()
    }

    // Returns a byte with the MSB bits set
    private fun getUpperMask(bits: Int): Byte {
        return (((1 shl bits) - 1) shl (8 - bits)).toByte()
    }

    fun validateSeedPhrase(phrase: String): BIP39Validation? {
        val normalizedPhrase = phrase.trim().lowercase()
        val words = normalizedPhrase.split(Regex("\\s+"))

        if (words.size < 12) {
            return BIP39Validation.TooShort
        }
        if (words.size > 24) {
            return BIP39Validation.TooLong
        }
        if (words.size.mod(3) != 0) {
            return BIP39Validation.BadLength
        }

        // 1-of-2048 is 11 bits
        val totalBits = words.size * 11
        val checksumBits = words.size / 3
        val entropyBits = totalBits - checksumBits

        // calculate the binary representation of the phrase
        var binaryPhrase = ""
        val invalidWords: MutableMap<Int, String> = mutableMapOf()
        words.forEachIndexed { index, word ->
            val indexInList = wordlist.indexOfFirst { it == word }
            if (indexInList > -1) {
                val binaryIndex = indexInList.toString(2).padStart(11, '0')
                binaryPhrase += binaryIndex
            } else {
                invalidWords[index] = word
            }
        }
        if (invalidWords.isNotEmpty()) {
            return BIP39Validation.InvalidWords(invalidWords)
        }

        // the layout of binaryPhrase is the entropy bits first followed by the checksum bits
        val entropy = BigInteger(binaryPhrase.substring(0 until entropyBits), 2)
        val checksum = BigInteger(binaryPhrase.substring(entropyBits), 2).toByte()

        // Calculate the expected checksum based on the entropy
        val expectedChecksum = MessageDigest
            .getInstance("SHA-256")
            .digest(entropy.toByteArrayNoSign()).first().and(getUpperMask(checksumBits))

        // Compare the calculated checksum with the expected checksum
        if (checksum != expectedChecksum) {
            return BIP39Validation.InvalidChecksum
        }

        return null
    }
}