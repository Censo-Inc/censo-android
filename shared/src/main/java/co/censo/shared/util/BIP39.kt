package co.censo.shared.util

import android.content.Context
import co.censo.shared.R
import co.censo.shared.data.cryptography.toBinaryString
import co.censo.shared.data.cryptography.toByteArrayNoSign
import co.censo.shared.util.BIP39.MAX_LENGTH
import co.censo.shared.util.BIP39.MIN_LENGTH
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

@Serializable
data class WordLists(
    val english: List<String>,
    val spanish: List<String>,
    val french: List<String>,
    val italian: List<String>,
    val portugese: List<String>,
    val czech: List<String>,
    val japanese: List<String>,
    val korean: List<String>,
    val chineseSimplified: List<String>,
    val chineseTraditional: List<String>,
)
sealed class BIP39InvalidReason {
     data class TooShort(val wordCount: Int): BIP39InvalidReason()
    data class TooLong(val wordCount: Int): BIP39InvalidReason()

    data class BadLength(val wordCount: Int): BIP39InvalidReason()

    data class InvalidWords(val wordsByIndex: Map<Int, String>): BIP39InvalidReason()

    object InvalidChecksum: BIP39InvalidReason()
}

fun BIP39InvalidReason.errorTitle() =
    when (this) {
        is BIP39InvalidReason.BadLength -> "Seed phrase is incorrect length"
        BIP39InvalidReason.InvalidChecksum -> "Seed phrase is invalid"
        is BIP39InvalidReason.InvalidWords -> "Seed phrase contains invalid words"
        is BIP39InvalidReason.TooLong -> "Seed phrase too long"
        is BIP39InvalidReason.TooShort -> "Seed phrase too short"
    }

fun BIP39InvalidReason.errorMessage() =
    when (this) {
        is BIP39InvalidReason.BadLength -> "Censo detected that you entered a seed phrase of the wrong size.\n\nSeed phrases are typically 12 or 24 words long."
        BIP39InvalidReason.InvalidChecksum -> "Censo detected that you entered a seed phrase that is invalid."
        is BIP39InvalidReason.InvalidWords -> "Censo detected that you entered a seed phrase that contains invalid words."
        is BIP39InvalidReason.TooLong -> "Censo detected that you entered a seed phrase that was ${this.wordCount - MAX_LENGTH} words long.\n\nSeed phrases are typically 12 or 24 words long."
        is BIP39InvalidReason.TooShort -> "Censo detected that you entered a seed phrase that was ${MIN_LENGTH - this.wordCount} word${if (MIN_LENGTH - this.wordCount > 1) "s" else ""} short.\n\nSeed phrases are typically 12 or 24 words long."
    }

object BIP39 {
    private lateinit var wordlists: Map<WordListLanguage, List<String>>

    fun setup(context: Context) {
        val wordListsFromJson = context.resources.openRawResource(R.raw.bip39words).let {
            Json.decodeFromStream<WordLists>(it)
        }
        this.wordlists = mapOf(
            WordListLanguage.English to wordListsFromJson.english,
            WordListLanguage.Spanish to wordListsFromJson.spanish,
            WordListLanguage.French to wordListsFromJson.french,
            WordListLanguage.Italian to wordListsFromJson.italian,
            WordListLanguage.Portugese to wordListsFromJson.portugese,
            WordListLanguage.Czech to wordListsFromJson.czech,
            WordListLanguage.Japanese to wordListsFromJson.japanese,
            WordListLanguage.Korean to wordListsFromJson.korean,
            WordListLanguage.ChineseTraditional to wordListsFromJson.chineseTraditional,
            WordListLanguage.ChineseSimplified to wordListsFromJson.chineseSimplified,
        )
    }

    enum class WordListLanguage {
        English,
        Spanish,
        French,
        Italian,
        Portugese,
        Czech,
        Japanese,
        Korean,
        ChineseTraditional,
        ChineseSimplified;

        companion object {
            fun fromWordListId(id: Byte): WordListLanguage {
                return when (id.toInt()) {
                    1 -> English
                    2 -> Spanish
                    3 -> French
                    4 -> Italian
                    5 -> Portugese
                    6 -> Czech
                    7 -> Japanese
                    8 -> Korean
                    9 -> ChineseTraditional
                    10 -> ChineseSimplified

                    else -> throw Exception("Unknown wordlist language id $id")
                }
            }
        }

        fun toId(): Byte {
            return when (this) {
                English -> 1
                Spanish -> 2
                French -> 3
                Italian -> 4
                Portugese -> 5
                Czech -> 6
                Japanese -> 7
                Korean -> 8
                ChineseTraditional -> 9
                ChineseSimplified -> 10
            }
        }

        fun displayName(): String {
            return when (this) {
                ChineseTraditional -> "Chinese (Traditional)"
                ChineseSimplified -> "Chinese (Simplified)"
                else -> this.name
            }
        }

        fun localizedDisplayName(): String {
            return when (this) {
                English -> "English"
                Spanish -> "Español"
                French -> "Français"
                Italian -> "Italiano"
                Portugese -> "Português"
                Czech -> "Čeština"
                Japanese -> "日本語"
                Korean -> "한국어"
                ChineseSimplified -> "中文(简体)"
                ChineseTraditional -> "中文(繁體)"
            }
        }
    }

    enum class WordCount(val value: Int) {
        Twelve(12),
        Fifteen(15),
        Eighteen(18),
        TwentyOne(21),
        TwentyFour(24)
    }

    const val MAX_LENGTH = 24
    const val MIN_LENGTH = 12
    const val COMMON_DENOMINATOR = 3

    // 1-of-2048 is 11 bits
    const val BITS_PER_WORD = 11

    const val BITS_PER_CHECKSUM_BIT = 4

    fun splitToWords(phrase: String): List<String> {
        val normalizedPhrase = phrase.trim().lowercase()
        return normalizedPhrase.split(Regex("[\\s\u3000]+"))
    }

    fun validateSeedPhrase(phrase: String): BIP39InvalidReason? {
        return validateSeedPhrase(splitToWords(phrase))
    }

    private fun getBitLengths(count: Int): Pair<Int, Int> {
        val totalBits = count * BITS_PER_WORD
        val checksumBits = count / 3
        val entropyBits = totalBits - checksumBits
        return Pair(entropyBits, checksumBits)
    }

    private fun computeChecksum(entropy: BigInteger, checksumBits: Int): UByte {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(entropy.toByteArrayNoSign(checksumBits * 4)).first().toUByte().toInt().shr(
                8 - checksumBits
            ).toByte().toUByte()
    }

    fun validateSeedPhrase(words: List<String>): BIP39InvalidReason? {
        if (words.size < MIN_LENGTH) {
            return BIP39InvalidReason.TooShort(words.size)
        }
        if (words.size > MAX_LENGTH) {
            return BIP39InvalidReason.TooLong(words.size)
        }
        if (words.size.mod(COMMON_DENOMINATOR) != 0) {
            return BIP39InvalidReason.BadLength(words.size)
        }

        val(entropyBits, checksumBits) = getBitLengths(words.size)

        val wordlist = wordlists[determineLanguage(words)]!!

        // calculate the binary representation of the phrase
        var binaryPhrase = ""
        val invalidWords: MutableMap<Int, String> = mutableMapOf()
        words.forEachIndexed { index, word ->
            val indexInList = wordlist.indexOfFirst { it == word }
            if (indexInList > -1) {
                val binaryIndex = indexInList.toString(2).padStart(BITS_PER_WORD, '0')
                binaryPhrase += binaryIndex
            } else {
                invalidWords[index] = word
            }
        }
        if (invalidWords.isNotEmpty()) {
            return BIP39InvalidReason.InvalidWords(invalidWords)
        }

        // the layout of binaryPhrase is the entropy bits first followed by the checksum bits
        val entropy = BigInteger(binaryPhrase.substring(0 until entropyBits), 2)
        val checksum = BigInteger(binaryPhrase.substring(entropyBits), 2).toByte().toUByte()

        // Calculate the expected checksum based on the entropy
        val expectedChecksum = computeChecksum(entropy, checksumBits)

        // Compare the calculated checksum with the expected checksum
        if (checksum != expectedChecksum) {
            return BIP39InvalidReason.InvalidChecksum
        }

        return null
    }

    fun determineLanguage(phrase: String): WordListLanguage {
        return determineLanguage(splitToWords(phrase))
    }

    fun determineLanguage(words: List<String>): WordListLanguage {
        words.forEach { word ->
            val candidates = wordlists.filter { (_, wordlist) ->  wordlist.contains(word) }.keys
            if (candidates.size == 1) {
                return candidates.first()
            } else if (word == words.last()) {
                return candidates.firstOrNull() ?: WordListLanguage.English
            }
        }
        return WordListLanguage.English
    }

    fun wordsToBinaryData(words: List<String>): ByteArray {
        val language = determineLanguage(words)
        val wordlist = wordlists[language]!!
        val binaryPhrase = words.joinToString("") { word ->
            wordlist
                .indexOf(word)
                .toString(radix = 2)
                .padStart(BITS_PER_WORD, '0')
        }

        val(entropyBits, checksumBits) = getBitLengths(words.size)
        val entropy = BigInteger(binaryPhrase.substring(0 until entropyBits), 2)
        val checksum = BigInteger(binaryPhrase.substring(entropyBits), 2).toByte().toUByte()

        val expectedChecksum = computeChecksum(entropy, checksumBits)
        if (checksum != expectedChecksum) {
            throw Exception("Checksums did not match when encoding phrase")
        }

        return byteArrayOf(language.toId()) + entropy.toByteArrayNoSign(checksumBits * 4)
    }

    fun wordLists(language: WordListLanguage): List<String>? {
        return wordlists[language]
    }

    fun binaryDataToWords(binaryData: ByteArray, language: WordListLanguage? = null): List<String> {
        val entropy = binaryData.slice(indices = 1 until binaryData.size).toByteArray()
        val checksumBits = entropy.size / BITS_PER_CHECKSUM_BIT
        val checksum = computeChecksum(BigInteger(1, entropy), checksumBits)
        val binaryPhrase = entropy.toBinaryString(entropy.size * 8) + checksum.toString(2).padStart(checksumBits, '0')
        val wordCount = binaryPhrase.length / BITS_PER_WORD

        val wordlist = language?.let {
            wordlists[it]!!
        } ?: run {
            wordlists[WordListLanguage.fromWordListId(binaryData[0])]!!
        }

        return (0 until wordCount).map { index ->
            wordlist[
                binaryPhrase.substring((BITS_PER_WORD * index) until (BITS_PER_WORD * (index + 1)))
                    .toUInt(2).toInt()
            ]
        }
    }

    fun generate(wordCount: WordCount, language: WordListLanguage): List<String> {
        val entropyBitsCount = wordCount.value / 3 * 32
        val entropy = ByteArray(entropyBitsCount / 8)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(entropy)
        return binaryDataToWords(byteArrayOf(language.toId()) + entropy)
    }
}