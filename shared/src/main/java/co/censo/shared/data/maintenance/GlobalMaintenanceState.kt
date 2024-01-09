package co.censo.shared.data.maintenance

import kotlinx.coroutines.flow.MutableStateFlow

object GlobalMaintenanceState {
    val isMaintenanceMode = MutableStateFlow(false)
}