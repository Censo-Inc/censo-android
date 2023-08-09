package co.censo.vault

import cash.z.ecc.android.bip39.Mnemonics
import java.util.Locale

object PhraseValidator {

    private val words = Mnemonics.getCachedWords(Locale.ENGLISH.language)

    private const val LONG_PHRASE = 24
    private const val SHORT_PHRASE = 12
    private const val NOT_ENOUGH_WORDS = "not enough words in phrase"
    private const val TOO_MANY_WORDS = "too many words in phrase"

    private fun getSuffixForWordIndex(index: Int): String =
        when (index) {
            1, 21 -> "st"
            2, 22 -> "nd"
            3, 23 -> "rd"
            in (4..20) -> "th"
            else -> "th"
        }

    fun isPhraseValid(phrase: String): Boolean {
        val phraseWords = phrase.split(" ")
        val wordCount = phraseWords.size

        if (wordCount != SHORT_PHRASE && wordCount != LONG_PHRASE) {
            return false
        }

        for ((_, word) in phraseWords.withIndex()) {
            if (word !in words) {
                return false
            }
        }

        return true
    }

    fun userInputValidPhrase(originalPhrase: String, inputtedPhrase: String): Boolean {
        if (originalPhrase == inputtedPhrase) {
            return true
        }

        val originalWords = originalPhrase.split(" ")
        val inputtedWords = inputtedPhrase.split(" ")

        val originalSize = originalWords.size
        val inputtedSize = inputtedWords.size

        if (originalSize != inputtedSize) {
            if (originalSize > inputtedSize && inputtedSize < SHORT_PHRASE) {
                throw Exception(NOT_ENOUGH_WORDS)
            }

            if (originalSize < inputtedSize && inputtedSize > LONG_PHRASE) {
                throw Exception(TOO_MANY_WORDS)
            }
        }

        for ((index, word) in originalWords.withIndex()) {
            val phraseTwoWord = inputtedWords[index]

            if (word != phraseTwoWord) {
                throw Exception("${index + 1}${getSuffixForWordIndex(index + 1)} does not match original phrase.")
            }
        }

        return false
    }

    fun format(text: String) =
        text.replace("[^A-Za-z ]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()

}