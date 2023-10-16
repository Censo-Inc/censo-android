package co.censo.vault

import Base58EncodedMasterPublicKey
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.censo.shared.SharedScreen
import co.censo.shared.data.storage.Storage
import co.censo.shared.presentation.entrance.EntranceScreen
import co.censo.vault.presentation.access_seed_phrases.AccessSeedPhrasesScreen
import co.censo.vault.presentation.activate_approvers.ActivateApproversScreen
import co.censo.vault.presentation.bip_39_detail.BIP39DetailScreen
import co.censo.vault.presentation.enter_phrase.EnterPhraseScreen
import co.censo.vault.presentation.home.HomeScreen
import co.censo.vault.presentation.home.Screen
import co.censo.vault.presentation.initial_plan_setup.InitialPlanSetupScreen
import co.censo.vault.presentation.lock_screen.LockScreenViewModel
import co.censo.vault.presentation.lock_screen.LockedScreen
import co.censo.vault.presentation.plan_setup.PlanSetupScreen
import co.censo.vault.presentation.recovery.RecoveryScreen
import co.censo.vault.presentation.vault.VaultScreen
import co.censo.vault.ui.theme.VaultTheme
import co.censo.vault.util.TestTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var storage: Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupPushChannel()

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            val navController = rememberNavController()

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
                    Box {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            CensoNavHost(navController = navController)
                        }

                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            LockedScreen()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        NavHost(
            navController = navController,
            startDestination = SharedScreen.EntranceRoute.route
        ) {
            composable(route = SharedScreen.EntranceRoute.route) {
                EntranceScreen(
                    navController = navController,
                    guardianEntrance = false
                )
            }
            composable(route = SharedScreen.HomeRoute.route) {
                HomeScreen(navController = navController)
            }
            composable(route = Screen.VaultScreen.route) {
                VaultScreen(navController = navController)
            }
            composable(route = Screen.RecoveryScreen.route) {
                RecoveryScreen(navController = navController)
            }
            composable(
                route = "${Screen.EnterPhraseRoute.route}/{${Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG}}"
            ) { backStackEntry ->
                EnterPhraseScreen(
                    navController = navController,
                    masterPublicKey = Base58EncodedMasterPublicKey(
                        backStackEntry.arguments?.getString(
                            Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG
                        )!!
                    )
                )
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
            composable(
                route = "${Screen.PlanSetupRoute.route}/{${Screen.PlanSetupRoute.EXISTING_SECURITY_PLAN_ARG}}",
                arguments = listOf(navArgument(Screen.PlanSetupRoute.EXISTING_SECURITY_PLAN_ARG) {
                    type = NavType.StringType
                })) { backStackEntry ->
                val securityPlanData =
                    backStackEntry.arguments?.getString(Screen.PlanSetupRoute.EXISTING_SECURITY_PLAN_ARG)
                PlanSetupScreen(
                    navController = navController,
                    existingSecurityPlan = securityPlanData?.let {
                        Json.decodeFromString(
                            securityPlanData
                        )
                    }
                )
            }
            composable(
                route = Screen.PlanSetupRoute.route
            ) {
                PlanSetupScreen(
                    navController = navController,
                    existingSecurityPlan = null
                )
            }
            composable(
                route = Screen.InitialPlanSetupRoute.route
            ) {
                InitialPlanSetupScreen(
                    navController = navController
                )
            }
            composable(route = Screen.ActivateApprovers.route) {
                ActivateApproversScreen(navController = navController)
            }
            composable(route = Screen.AccessSeedPhrases.route) {
                AccessSeedPhrasesScreen(navController = navController)
            }
        }
    }

    private fun setupPushChannel() {
        val channelId = getString(R.string.default_notification_channel_id)
        val channelName = getString(R.string.default_notification_channel_name)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}