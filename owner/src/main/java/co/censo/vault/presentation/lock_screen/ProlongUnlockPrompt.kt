package co.censo.vault.presentation.lock_screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.components.recovery.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// we will show the prompt when it's less that this duration until unlock expires
private val timeRemainingWhenProlongationPossible = 3.minutes

data class ProlongUnlockPromptState(
    val timeRemaining: Duration = 0.seconds,
    val prolongationPromptDismissed: Boolean = false,
    val prolongationFailed: Boolean = false
) {
    val showProlongationPrompt: Boolean =
        timeRemaining.inWholeSeconds > 0 &&
                timeRemaining <= timeRemainingWhenProlongationPossible &&
                !prolongationPromptDismissed
}

@HiltViewModel
class ProlongUnlockPromptViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {
    var state by mutableStateOf(ProlongUnlockPromptState())
        private set

    fun onStart(locksAt: Instant) {
        state = state.copy(
            timeRemaining = locksAt - Clock.System.now(),
            prolongationPromptDismissed = false
        )
    }

    suspend fun onTick(locksAt: Instant, onTimeOut: () -> Unit) {
        val now =  Clock.System.now()
        if (now >= locksAt) {
            onTimeOut()
        } else {
            delay(1.seconds.toJavaDuration())
            state = state.copy(timeRemaining = locksAt - Clock.System.now())
        }

        if (state.prolongationPromptDismissed && state.timeRemaining > timeRemainingWhenProlongationPossible) {
            state = state.copy(prolongationPromptDismissed = false)
        }
    }

    fun dismissProlongationPrompt() {
        state = state.copy(prolongationPromptDismissed = true)
    }

    fun prolongUnlock() {
        dismissProlongationPrompt()

        viewModelScope.launch {
            val response: Resource<ProlongUnlockApiResponse> = ownerRepository.prolongUnlock()

            if (response is Resource.Success) {
                ownerStateFlow.tryEmit(response.map { it.ownerState })
            } else {
                state = state.copy(
                    prolongationFailed = true,
                    prolongationPromptDismissed = false
                )
            }
        }
    }

    fun dismissErrorAlert() {
        state = state.copy(prolongationFailed = false)
    }
}

@Composable
fun ProlongUnlockPrompt(
    locksAt: Instant,
    onTimeOut: () -> Unit,
    viewModel: ProlongUnlockPromptViewModel = hiltViewModel()
) {
    val state = viewModel.state

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(locksAt)
        onDispose {}
    }

    LaunchedEffect(key1 = state.timeRemaining) {
        viewModel.onTick(locksAt, onTimeOut)
    }

    if (state.prolongationFailed) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissErrorAlert()
            },
            title = {
                Text(text = "Failed to extend session")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White),
                    onClick = viewModel::dismissErrorAlert
                ) {
                    Text("Ok")
                }
            }
        )
    } else if (state.showProlongationPrompt) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(text = "Extend session?")
            },
            text = {
                Text("For security, your session will expire in ${formatDuration(state.timeRemaining)}")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White),
                    onClick = viewModel::prolongUnlock
                ) {
                    Text("Extend")
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White),
                    onClick = viewModel::dismissProlongationPrompt
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}