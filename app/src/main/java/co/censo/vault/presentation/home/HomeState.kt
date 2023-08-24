package co.censo.vault.presentation.home

import co.censo.vault.data.Resource
import co.censo.vault.data.storage.BIP39Phrases

data class HomeState(
    val phrases: BIP39Phrases = emptyMap(),
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
)