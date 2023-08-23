package co.censo.vault.presentation.main

import co.censo.vault.data.Resource
import co.censo.vault.util.BioPromptReason
import co.censo.vault.util.BiometricUtil

data class MainState(
    val biometryStatus: BiometricUtil.Companion.BiometricsStatus? = null,
    val blockAppUI: BlockAppUI = BlockAppUI.NONE,
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val bioPromptReason: BioPromptReason = BioPromptReason.UNINITIALIZED,
    val tooManyAttempts: Boolean = false,
    val biometryInvalidated: Resource<Unit> = Resource.Uninitialized
)

enum class BlockAppUI {
    BIOMETRY_DISABLED, FOREGROUND_BIOMETRY, NONE
}

