package co.censo.vault.presentation.main

import co.censo.vault.Resource

data class MainState(
    val biometryStatus: BiometricUtil.Companion.BiometricsStatus? = null,
    val blockAppUI: BlockAppUI = BlockAppUI.NONE,
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val tooManyAttempts: Boolean = false
)

enum class BlockAppUI {
    BIOMETRY_DISABLED, FOREGROUND_BIOMETRY, NONE
}