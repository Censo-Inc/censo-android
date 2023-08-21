package co.censo.vault.presentation.home

import co.censo.vault.data.storage.BIP39Phrases

data class HomeState(
    val phrases: BIP39Phrases = emptyMap()
)