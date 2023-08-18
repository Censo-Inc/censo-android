package co.censo.vault

import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricPrompt
import co.censo.vault.CryptographyManagerImpl.Companion.STATIC_DEVICE_KEY_CHECK
import co.censo.vault.presentation.main.BlockAppUI
import co.censo.vault.presentation.main.MainViewModel
import co.censo.vault.storage.Storage
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BaseViewModelTest() {

    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var cryptographyManager: CryptographyManager

    private val dispatcher = StandardTestDispatcher()

    private lateinit var mainViewModel: MainViewModel

    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(dispatcher)

        mainViewModel = MainViewModel(
            storage = storage,
            cryptographyManager = cryptographyManager
        )
    }

    @Test
    fun `if device does not have biometric then keep blocking state`() = runTest {
        mainViewModel.onForeground(BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE)

        assertEquals(mainViewModel.state.biometryStatus,
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE
        )
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.BIOMETRY_DISABLED)
    }

    @Test
    fun `if device does not have biometry enabled then keep blocking state`() = runTest {
        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_DISABLED
        )

        assertEquals(mainViewModel.state.biometryStatus,
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_DISABLED
        )
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.BIOMETRY_DISABLED)
    }

    @Test
    fun `if device has biometry enabled but no device key, do not trigger biometry`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { false }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        verify(cryptographyManager).createDeviceKeyIfNotExists()

        assert(mainViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.NONE)
    }

    @Test
    fun `if device has biometry enabled but no stored phrases, do not trigger biometry`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { false }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        verify(cryptographyManager).createDeviceKeyIfNotExists()

        assert(mainViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.NONE)
    }

    @Test
    fun `if device has biometry enabled, a device key and stored phrases, then trigger biometry`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.blockUIStatus(), BlockAppUI.FOREGROUND_BIOMETRY)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)
    }

    @Test
    fun `on basic biometry failure, set error state`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)

        mainViewModel.onBiometryFailed(-1)

        assert(mainViewModel.state.bioPromptTrigger is Resource.Error)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)
        assert(!mainViewModel.state.tooManyAttempts)
    }

    @Test
    fun `on lockout biometry failure, set error state and too many attempts flag`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)

        mainViewModel.onBiometryFailed(BiometricPrompt.ERROR_LOCKOUT)

        assert(mainViewModel.state.bioPromptTrigger is Resource.Error)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)
        assert(mainViewModel.state.tooManyAttempts)
    }

    @Test
    fun `on permanent lockout biometry failure, set error state and too many attempts flag`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)

        mainViewModel.onBiometryFailed(BiometricPrompt.ERROR_LOCKOUT_PERMANENT)

        assert(mainViewModel.state.bioPromptTrigger is Resource.Error)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)
        assert(mainViewModel.state.tooManyAttempts)
    }

    @Test
    fun `device can encrypt and decrypt data with device key after biometry approval`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }
        val encryptedData =
            Base64.getDecoder().decode("dQpuKzb6AU6u5HdvxJ5r5Z4Zeg6Xqdt6UllqdaR3OaBeVNwxL6N8pGju3bqXhvXoperI1iPm0tXIPSSUxxIbmfqWFy4hBfzS1hcIw9yN9xUq0t")

        whenever(cryptographyManager.encryptData(CryptographyManagerImpl.STATIC_DEVICE_KEY_CHECK)).then {
            encryptedData
        }

        whenever(cryptographyManager.decryptData(encryptedData)).then {
            STATIC_DEVICE_KEY_CHECK.toByteArray()
        }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.blockUIStatus(), BlockAppUI.FOREGROUND_BIOMETRY)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)

        mainViewModel.onBiometryApproved()

        advanceUntilIdle()

        assert(mainViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assert(mainViewModel.state.blockAppUI == BlockAppUI.NONE)
    }

    @Test
    fun `device fails encrypt and decrypt data with device key then show blocking UI`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }
        val encryptedData =
            Base64.getDecoder().decode("dQpuKzb6AU6u5HdvxJ5r5Z4Zeg6Xqdt6UllqdaR3OaBeVNwxL6N8pGju3bqXhvXoperI1iPm0tXIPSSUxxIbmfqWFy4hBfzS1hcIw9yN9xUq0t")

        whenever(cryptographyManager.encryptData(STATIC_DEVICE_KEY_CHECK)).then {
            encryptedData
        }

        whenever(cryptographyManager.decryptData(encryptedData)).then {
            "not the static device key check".toByteArray()
        }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.blockUIStatus(), BlockAppUI.FOREGROUND_BIOMETRY)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)

        mainViewModel.onBiometryApproved()

        advanceUntilIdle()

        assert(mainViewModel.state.bioPromptTrigger is Resource.Error)
        assert(mainViewModel.state.blockAppUI == BlockAppUI.FOREGROUND_BIOMETRY)
    }

    @Test
    fun `device not authorized to encrypt or decrypt then we wipe key data`() = runTest {
        whenever(cryptographyManager.deviceKeyExists()).then { true }
        whenever(storage.storedPhrasesIsNotEmpty()).then { true }
        val encryptedData =
            Base64.getDecoder().decode("dQpuKzb6AU6u5HdvxJ5r5Z4Zeg6Xqdt6UllqdaR3OaBeVNwxL6N8pGju3bqXhvXoperI1iPm0tXIPSSUxxIbmfqWFy4hBfzS1hcIw9yN9xUq0t")

        whenever(cryptographyManager.encryptData(STATIC_DEVICE_KEY_CHECK)).then {
            encryptedData
        }

        whenever(cryptographyManager.decryptData(encryptedData)).then {
            throw UserNotAuthenticatedException()
        }

        mainViewModel.onForeground(
            BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED
        )

        assert(mainViewModel.state.bioPromptTrigger is Resource.Success)
        assertEquals(mainViewModel.blockUIStatus(), BlockAppUI.FOREGROUND_BIOMETRY)
        assertEquals(mainViewModel.state.blockAppUI, BlockAppUI.FOREGROUND_BIOMETRY)
        assert(mainViewModel.state.biometryInvalidated is Resource.Uninitialized)

        mainViewModel.onBiometryApproved()

        advanceUntilIdle()

        verify(cryptographyManager).deleteDeviceKeyIfPresent()
        verify(storage).clearStoredPhrases()

        assert(mainViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assert(mainViewModel.state.blockAppUI == BlockAppUI.NONE)
        assert(mainViewModel.state.biometryInvalidated is Resource.Success)
    }
}