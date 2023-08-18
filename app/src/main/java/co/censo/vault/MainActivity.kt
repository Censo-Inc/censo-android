package co.censo.vault

import co.censo.vault.util.BiometricUtil
import BlockingUI
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.navigation.navDeepLink
import co.censo.vault.data.Resource
import co.censo.vault.presentation.add_bip39.AddBIP39Screen
import co.censo.vault.presentation.bip_39_detail.BIP39DetailScreen
import co.censo.vault.presentation.components.OnLifecycleEvent
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationScreen
import co.censo.vault.presentation.home.HomeScreen
import co.censo.vault.presentation.home.Screen
import co.censo.vault.presentation.home.Screen.Companion.DL_TOKEN_KEY
import co.censo.vault.presentation.home.Screen.Companion.GUARDIAN_DEEPLINK_ACCEPTANCE
import co.censo.vault.presentation.main.MainViewModel
import co.censo.vault.ui.theme.VaultTheme
import co.censo.vault.util.TestTag
import co.censo.vault.util.popUpToTop
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            composable(
                "$GUARDIAN_DEEPLINK_ACCEPTANCE?$DL_TOKEN_KEY={$DL_TOKEN_KEY}",
                deepLinks = listOf(navDeepLink {
                    uriPattern = "vault://guardian/{$DL_TOKEN_KEY}"
                }),
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString(DL_TOKEN_KEY)
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Passed in this data on deep link: $token")
                }
            }

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
            composable(route = Screen.GuardianInvitationRoute.route) {
                GuardianInvitationScreen(navController = navController)
            }
        }
    }
}