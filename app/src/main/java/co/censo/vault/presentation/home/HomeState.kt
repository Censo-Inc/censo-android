package co.censo.vault.presentation.home

import co.censo.shared.data.Resource
import co.censo.shared.data.storage.BIP39Phrases

data class HomeState(
    val phrases: BIP39Phrases = emptyMap(),
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
)