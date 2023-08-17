package co.censo.vault.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storage: Storage
) : ViewModel() {

    var state by mutableStateOf(HomeState())
        private set

    fun onStart() {
        state = state.copy(phrases = storage.retrieveBIP39Phrases())
    }
}