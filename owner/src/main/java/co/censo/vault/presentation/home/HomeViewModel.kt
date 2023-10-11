package co.censo.vault.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(HomeState())
        private set

    fun onStart() {
        val userEditingPlan = ownerRepository.isUserEditingSecurityPlan()

        state = state.copy(
            userEditingPlan = userEditingPlan
        )

        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            state = state.copy(
                ownerStateResource = ownerStateResource,
            )
        }
    }


    fun resetOwnerState() {
        state = state.copy(ownerStateResource = Resource.Uninitialized)
    }
}