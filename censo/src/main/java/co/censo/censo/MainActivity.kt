package co.censo.censo

import Base58EncodedMasterPublicKey
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.censo.shared.SharedScreen
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.presentation.entrance.EntranceScreen
import co.censo.censo.presentation.access_seed_phrases.AccessSeedPhrasesScreen
import co.censo.censo.presentation.enter_phrase.EnterPhraseScreen
import co.censo.censo.presentation.welcome.WelcomeScreen
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.main.MainVaultScreen
import co.censo.censo.presentation.initial_plan_setup.InitialPlanSetupScreen
import co.censo.censo.presentation.lock_screen.LockedScreen
import co.censo.censo.presentation.plan_setup.PlanSetupScreen
import co.censo.censo.presentation.access_approval.AccessApprovalScreen
import co.censo.censo.presentation.routing.OwnerRoutingScreen
import co.censo.censo.ui.theme.VaultTheme
import co.censo.censo.util.TestTag
import co.censo.shared.util.sendError
import com.raygun.raygun4android.RaygunClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var storage: SecurePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRayGunCrashReporting()
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
            composable(
                SharedScreen.OwnerRoutingScreen.route
            ) {
                OwnerRoutingScreen(navController = navController)
            }
            composable(route = Screen.OwnerWelcomeScreen.route) {
                WelcomeScreen(navController = navController)
            }
            composable(route = Screen.OwnerVaultScreen.route) {
                MainVaultScreen(navController = navController)
            }
            composable(route = Screen.AccessApproval.route) {
                AccessApprovalScreen(navController = navController)
            }
            composable(
                route = "${Screen.EnterPhraseRoute.route}/{${Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG}}/{${Screen.EnterPhraseRoute.WELCOME_FLOW_ARG}}"
            ) { backStackEntry ->
                EnterPhraseScreen(
                    navController = navController,
                    masterPublicKey = Base58EncodedMasterPublicKey(
                        backStackEntry.arguments?.getString(
                            Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG
                        )!!
                    ),
                    welcomeFlow = (backStackEntry.arguments?.getString(
                        Screen.EnterPhraseRoute.WELCOME_FLOW_ARG
                    ) ?: false.toString()).toBoolean()
                )
            }
            composable(
                route = "${Screen.PlanSetupRoute.route}/{${Screen.PlanSetupRoute.WELCOME_FLOW_ARG}}"
            ) { backStackEntry ->
                PlanSetupScreen(
                    navController = navController,
                    welcomeFlow = (backStackEntry.arguments?.getString(
                        Screen.PlanSetupRoute.WELCOME_FLOW_ARG
                    ) ?: false.toString()).toBoolean()
                )
            }
            composable(
                route = Screen.InitialPlanSetupRoute.route
            ) {
                InitialPlanSetupScreen(
                    navController = navController
                )
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

    private fun setupRayGunCrashReporting() {
        RaygunClient.init(application);
        RaygunClient.enableCrashReporting();
    }
}