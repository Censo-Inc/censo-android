import BiometricUtil.Companion.FAIL_ERROR
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import co.censo.vault.BuildConfig
import co.censo.vault.R


object BiometricUtil {

    object Companion {
        const val FAIL_ERROR = -1

        enum class BiometricsStatus {
            BIOMETRICS_ENABLED, BIOMETRICS_DISABLED, BIOMETRICS_NOT_AVAILABLE
        }
    }

    fun createPromptInfo(context: Context) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.complete_biometry))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()

    fun createBioPrompt(
        fragmentActivity: FragmentActivity,
        onSuccess: (cryptoObject: BiometricPrompt.CryptoObject?) -> Unit,
        onFail: (errorCode: Int) -> Unit
    ) =
        BiometricPrompt(fragmentActivity, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onFail(errorCode)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result.cryptoObject)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFail(FAIL_ERROR)
            }
        })

    fun handleBioPromptOnFail(context: Context, errorCode: Int, handleFailure: () -> Unit) {
        val bioPromptFailedReason = getBioPromptFailedReason(errorCode)

        val message = getBioPromptMessage(bioPromptFailedReason, context)

        if (message.isNotEmpty()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

        handleFailure()
    }

    fun getBioPromptFailedReason(errorCode: Int): BioPromptFailedReason =
        when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BioPromptFailedReason.FAILED_TOO_MANY_ATTEMPTS
            else -> BioPromptFailedReason.BIOMETRY_FAILED
        }

    private fun getBioPromptMessage(
        bioPromptReason: BioPromptFailedReason,
        context: Context
    ): String =
        when (bioPromptReason) {
            BioPromptFailedReason.BIOMETRY_FAILED -> ""
            BioPromptFailedReason.CIPHER_NULL -> context.getString(R.string.key_management_bio_canceled)
            BioPromptFailedReason.FAILED_TOO_MANY_ATTEMPTS -> context.getString(R.string.too_many_failed_attempts)
        }

    enum class BioPromptReason {
        UNINITIALIZED, FOREGROUND_RETRIEVAL
    }

    enum class BioPromptFailedReason {
        BIOMETRY_FAILED, CIPHER_NULL, FAILED_TOO_MANY_ATTEMPTS
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