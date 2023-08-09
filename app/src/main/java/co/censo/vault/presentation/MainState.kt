package co.censo.vault.presentation

data class MainState(
    val biometryStatus: BiometricUtil.Companion.BiometricsStatus? = null,
    val blockAppUI: BlockAppUI = BlockAppUI.NONE
)

enum class BlockAppUI {
    BIOMETRY_DISABLED, NONE
}