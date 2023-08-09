package co.censo.vault

import BlockingUI
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.censo.vault.presentation.MainViewModel
import co.censo.vault.presentation.add_bip39.AddBIP39Screen
import co.censo.vault.presentation.components.OnLifecycleEvent
import co.censo.vault.presentation.home.HomeScreen
import co.censo.vault.presentation.home.Screen
import co.censo.vault.ui.theme.VaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            val mainState = mainViewModel.state

            VaultTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
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
                        biometryStatus = mainState.biometryStatus,
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
        }
    }
}