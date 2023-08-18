package co.censo.vault

import BiometricUtil
import BlockingUI
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.censo.vault.presentation.add_bip39.AddBIP39Screen
import co.censo.vault.presentation.bip_39_detail.BIP39DetailScreen
import co.censo.vault.presentation.components.OnLifecycleEvent
import co.censo.vault.presentation.home.HomeScreen
import co.censo.vault.presentation.home.Screen
import co.censo.vault.presentation.main.MainViewModel
import co.censo.vault.storage.BIP39Phrases
import co.censo.vault.storage.EncryptedBIP39
import co.censo.vault.ui.theme.VaultTheme
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phrases = mapOf("test1" to
                EncryptedBIP39(
                    base64 = "BKCv4TeFKsMzfc6eni1BI1qS2pKD0XcE7kezRIMXuKdvCJK4AmfLdTX4ha0lFLF9HoLjzHp2W6uBSA8OUUJfvx0cZrUpryU6ex1wKKiyw8/9rd4gHJydYy0NyovdvaWA/ieZU5qpDBj7vCsoChERVm/AM/j5xvbSQe5tjUiPDkzh3UcE70GmhPIw+6PVHjXMNGroqqgwWIGKoY7asK8m41+dQpuKzb6AU6u5HdvxJ5r5Z4Zeg6Xqdt6UllqdaR3OaBeVNwxL6N8pGju3bqXhvXoperI1iPm0tXIPSSUxxIbmfqWFy4hBfzS1hcIw9yN9xUq0t+z8xiHXJEGArSak6Npx+TwObZfK0jsG1+WQ55nsFxXJ5eST1ayZvuPaWA==",
                    createdAt = ZonedDateTime.now()
                )
        )

        val kotlinx = Json.encodeToString(phrases)
        val jsonMapped = jsonMapper.writeValueAsString(phrases)

        vaultLog(message = "kotlinx encoded: $kotlinx")
        vaultLog(message = "jackson encoded: $jsonMapped")

        val recreatedKotlinX = Json.decodeFromString<BIP39Phrases>(kotlinx)
        val recreatedJsonMapped : BIP39Phrases = jsonMapper.readValue(jsonMapped)

        vaultLog(message = "----------------------------------------------")

        vaultLog(message = "kotlinx recreated: $recreatedKotlinX")
        vaultLog(message = "jackson recreated: $recreatedJsonMapped")

        vaultLog(message = "----------------------------------------------")

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            val mainState = mainViewModel.state

            LaunchedEffect(key1 = mainState) {

                if (mainState.bioPromptTrigger is Resource.Success) {

                    val promptInfo = BiometricUtil.createPromptInfo(context = context)

                    val bioPrompt = BiometricUtil.createBioPrompt(
                        fragmentActivity = this@MainActivity,
                        onSuccess = {
                            mainViewModel.onBiometryApproved()
                        },
                        onFail = {
                            BiometricUtil.handleBioPromptOnFail(
                                context = context,
                                errorCode = it
                            ) {
                                mainViewModel.onBiometryFailed(errorCode = it)
                            }
                        }
                    )

                    bioPrompt.authenticate(promptInfo)
                }

                if (mainState.biometryInvalidated is Resource.Success) {
                    Toast.makeText(
                        context,
                        getString(R.string.biometry_invalidated_keys_no_longer_accessible),
                        Toast.LENGTH_LONG
                    ).show()

                    navController.navigate(Screen.HomeRoute.route) {
                        launchSingleTop = true
                        popUpToTop()
                    }
                    mainViewModel.resetBiometryInvalidated()
                }
            }

            VaultTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            testTag = TestTag.main_activity_surface_container
                        },
                    color = MaterialTheme.colorScheme.background
                ) {
                    CensoNavHost(navController = navController)

                    OnLifecycleEvent { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START
                            -> {
                                mainViewModel.onForeground(
                                    BiometricUtil.checkForBiometricFeaturesOnDevice(context),
                                )
                            }

                            else -> Unit
                        }
                    }

                    val blockAppUI = mainViewModel.blockUIStatus()

                    BlockingUI(
                        blockAppUI = blockAppUI,
                        bioPromptTrigger = mainState.bioPromptTrigger,
                        biometryUnavailable = mainState.tooManyAttempts,
                        biometryStatus = mainState.biometryStatus,
                        retry = mainViewModel::launchBlockingForegroundBiometryRetrieval
                    )
                }
            }
        }
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        NavHost(navController = navController, startDestination = Screen.HomeRoute.route) {
            composable(route = Screen.HomeRoute.route) {
                HomeScreen(navController = navController)
            }
            composable(route = Screen.AddBIP39Route.route) {
                AddBIP39Screen(navController = navController)
            }
            composable(
                route = "${Screen.BIP39DetailRoute.route}/{${Screen.BIP39DetailRoute.BIP_39_NAME_ARG}}",
                arguments = listOf(navArgument(Screen.BIP39DetailRoute.BIP_39_NAME_ARG) {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val nameArgument =
                    backStackEntry.arguments?.getString(Screen.BIP39DetailRoute.BIP_39_NAME_ARG) as String
                BIP39DetailScreen(navController = navController, bip39Name = nameArgument)
            }

        }
    }
}