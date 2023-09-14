package co.censo.vault

import co.censo.vault.data.PhraseValidator
import org.junit.Test
import org.junit.Assert.assertFalse

class PhraseValidatorTest {

    val twentyFourWordPhrase =
        "echo flat forget radio apology old until elite keep fine clock parent cereal ticket dutch whisper flock junior pet six uphold gorilla trend spare"

    val twelveWordPhrase =
        "butter same scatter question funny beef grid beef century pulse vendor connect"

    val invalidWord = "belly"

    @Test
    fun test24WordValidPhrase() {
        assert(PhraseValidator.isPhraseValid(twentyFourWordPhrase))
    }

    @Test
    fun test12WordValidPhrase() {
        assert(PhraseValidator.isPhraseValid(twelveWordPhrase))
    }

    @Test
    fun testPhraseBetween12and24() {
        val words = twentyFourWordPhrase.split(" ").toMutableList()
        words.removeAt(0)

        val validPhrase = PhraseValidator.isPhraseValid(words.joinToString(" "))
        assertFalse(validPhrase)
    }

    @Test
    fun testPhraseBelow12() {
        val words = twelveWordPhrase.split(" ").toMutableList()
        words.removeAt(0)

        val validPhrase = PhraseValidator.isPhraseValid(words.joinToString(" "))
        assertFalse(validPhrase)
    }

    @Test
    fun testPhraseAbove24() {
        val words = twentyFourWordPhrase.split(" ").toMutableList()
        words.add("hello")

        val validPhrase = PhraseValidator.isPhraseValid(words.joinToString(" "))
        assertFalse(validPhrase)
    }

    @Test
    fun invalidWordAt5thSpot() {
        val words = twentyFourWordPhrase.split(" ").toMutableList()
        words[4] = (invalidWord)

        val validPhrase = PhraseValidator.isPhraseValid(words.joinToString(" "))
        assertFalse(validPhrase)
    }
}