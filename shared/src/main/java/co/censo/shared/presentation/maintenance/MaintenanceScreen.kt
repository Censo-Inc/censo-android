package co.censo.shared.presentation.maintenance

import MessageText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.R
import co.censo.shared.data.maintenance.GlobalMaintenanceState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceState(
    val maintenanceMode: Boolean = false
)

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    Box(modifier = Modifier
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { }
    ) {
        if (state.maintenanceMode) {
            MaintenanceModeUI()
        } else {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val timer: VaultCountDownTimer,
    private val ownerRepository: OwnerRepository,
) : ViewModel() {
    var state by mutableStateOf(MaintenanceState())
        private set

    private val maintenanceModeDelayMillis = 1000L

    fun onStart() {
        viewModelScope.launch {
            GlobalMaintenanceState.isMaintenanceMode.collectLatest { maintenanceMode: Boolean ->
                onMaintenanceMode(maintenanceMode)
            }
        }
    }

    private suspend fun onMaintenanceMode(maintenanceMode: Boolean) {
        if (maintenanceMode) {
            timer.start(
                interval = CountDownTimerImpl.Companion.MAINTENANCE_MODE_COUNTDOWN,
                onTickCallback = {
                    viewModelScope.launch {
                        // http response code will be processed in ApiService updating GlobalMaintenanceState
                        ownerRepository.retrieveUser()
                    }
                }
            )
            state = state.copy(maintenanceMode = true)
        } else {
            timer.stop()

            // Delay to allow for retry
            delay(maintenanceModeDelayMillis)
            state = state.copy(maintenanceMode = false)
        }
    }
}

@Composable
fun MaintenanceModeUI() {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = SharedColors.MaintenanceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MessageText(
            message = stringResource(R.string.under_maintenance)
        )
    }
}


@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun MaintenanceModeUIPreview() {
    MaintenanceModeUI()
}
