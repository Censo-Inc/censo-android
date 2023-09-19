package co.censo.vault.presentation.bip_39_detail

data class BIP39DetailState(
    val name: String = "",
    val bip39Phrase: String = "",
    val currentWordIndex: Int = 0,
) {

    val phraseWordCount = bip39Phrase.split(" ").size
    val lastWordIndex = phraseWordCount - 1

    val lastSetStartIndex = phraseWordCount - CHANGE_AMOUNT

    companion object {
        const val FIRST_WORD_INDEX = 0
        const val CHANGE_AMOUNT = 4
    }
}
