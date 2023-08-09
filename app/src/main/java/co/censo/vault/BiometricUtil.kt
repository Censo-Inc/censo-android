import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import co.censo.vault.BuildConfig


object BiometricUtil {

    object Companion {
        enum class BiometricsStatus {
            BIOMETRICS_ENABLED, BIOMETRICS_DISABLED, BIOMETRICS_NOT_AVAILABLE
        }
    }

    fun checkForBiometricFeaturesOnDevice(context: Context): Companion.BiometricsStatus {

        val hasStrongBox = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_STRONGBOX_KEYSTORE
        )

        if (!hasStrongBox && !BuildConfig.DEBUG) {
            return Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE
        }

        //Checking the biometric status of the device, if it is enabled, disabled, or not available
        return when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Companion.BiometricsStatus.BIOMETRICS_ENABLED
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Companion.BiometricsStatus.BIOMETRICS_DISABLED
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE
            }
            else -> {
                Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE
            }
        }
    }
}