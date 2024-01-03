package co.censo.censo

import Base58EncodedMasterPublicKey
import Base64EncodedData
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.Screen.LoginIdResetRoute.DL_RESET_TOKEN_KEY
import co.censo.censo.presentation.Screen.Companion.CENSO_IMPORT_DEEPLINK
import co.censo.censo.presentation.Screen.Companion.IMPORT_KEY_KEY
import co.censo.censo.presentation.Screen.Companion.NAME_KEY
import co.censo.censo.presentation.Screen.Companion.SIGNATURE_KEY
import co.censo.censo.presentation.Screen.Companion.TIMESTAMP_KEY
import co.censo.censo.presentation.access_approval.AccessApprovalScreen
import co.censo.censo.presentation.access_seed_phrases.AccessSeedPhrasesScreen
import co.censo.censo.presentation.enter_phrase.EnterPhraseScreen
import co.censo.censo.presentation.entrance.OwnerEntranceScreen
import co.censo.censo.presentation.import_phrase.PhraseImportScreen
import co.censo.censo.presentation.initial_plan_setup.InitialPlanSetupScreen
import co.censo.censo.presentation.lock_screen.LockedScreen
import co.censo.censo.presentation.login_id_reset.LoginIdResetScreen
import co.censo.censo.presentation.main.BottomNavItem
import co.censo.censo.presentation.main.MainVaultScreen
import co.censo.censo.presentation.owner_key_recovery.OwnerKeyRecoveryScreen
import co.censo.censo.presentation.owner_key_validation.ValidateApproverKeyScreen
import co.censo.censo.presentation.paywall.PaywallScreen
import co.censo.censo.presentation.plan_finalization.ReplacePolicyScreen
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.presentation.plan_setup.PolicySetupScreen
import co.censo.censo.ui.theme.VaultTheme
import co.censo.censo.util.TestTag
import co.censo.shared.DeepLinkURI.OWNER_LOGIN_ID_RESET_URI
import co.censo.shared.DeepLinkURI
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.Import
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.presentation.maintenance.MaintenanceScreen
import co.censo.shared.util.StrongboxUI
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var storage: SecurePreferences

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
                        CensoNavHost(navController = navController)

                        ValidateApproverKeyScreen(navController = navController)

                        LockedScreen()

                        PaywallScreen(navController = navController)

                        MaintenanceScreen()
                    }

                    StrongboxUI()
                }
            }
        }
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        val bottomNavItem: MutableState<BottomNavItem> =
            remember { mutableStateOf(BottomNavItem.Home) }

        NavHost(
            navController = navController,
            startDestination = Screen.EntranceRoute.route
        ) {
            composable(route = Screen.EntranceRoute.route) {
                OwnerEntranceScreen(navController = navController)
            }
            composable(route = Screen.OwnerKeyRecoveryRoute.route) {
                OwnerKeyRecoveryScreen(navController = navController)
            }
            composable(route = Screen.OwnerVaultScreen.route) {
                MainVaultScreen(
                    selectedBottomNavItem = bottomNavItem,
                    navController = navController
                )
            }
            composable(route = "${Screen.AccessApproval.route}/{${Screen.AccessApproval.ACCESS_INTENT_ARG}}") { backStackEntry ->
                AccessApprovalScreen(
                    navController = navController,
                    accessIntent = backStackEntry.arguments?.getString(Screen.AccessApproval.ACCESS_INTENT_ARG)
                        ?.let { AccessIntent.valueOf(it) } ?: AccessIntent.AccessPhrases,
                )
            }
            composable(
                route = "${Screen.EnterPhraseRoute.route}/{${Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG}}/{${Screen.EnterPhraseRoute.WELCOME_FLOW_ARG}}/{${Screen.EnterPhraseRoute.IMPORTING_PHRASE_ARG}}/{${Screen.EnterPhraseRoute.WORDS_ARG}}"
            ) { backStackEntry ->
                val importingPhrase = (backStackEntry.arguments?.getString(Screen.EnterPhraseRoute.IMPORTING_PHRASE_ARG)
                    ?.toBoolean()) ?: false

                val words = (backStackEntry.arguments?.getString(Screen.EnterPhraseRoute.WORDS_ARG) ?: "").split("_")

                EnterPhraseScreen(
                    navController = navController,
                    masterPublicKey = Base58EncodedMasterPublicKey(
                        backStackEntry.arguments?.getString(
                            Screen.EnterPhraseRoute.MASTER_PUBLIC_KEY_NAME_ARG
                        )!!
                    ),
                    welcomeFlow = (backStackEntry.arguments?.getString(Screen.EnterPhraseRoute.WELCOME_FLOW_ARG)
                        ?.toBoolean()) ?: false,
                    importingPhrase = importingPhrase,
                    words = words
                )
            }
            composable(
                route = "${Screen.PolicySetupRoute.route}/{${Screen.PolicySetupRoute.SETUP_ACTION_ARG}}"
            ) { backStackEntry ->
                PolicySetupScreen(
                    navController = navController,
                    policySetupAction = backStackEntry.arguments?.getString(Screen.PolicySetupRoute.SETUP_ACTION_ARG)
                        ?.let { PolicySetupAction.valueOf(it) } ?: PolicySetupAction.AddApprovers
                )
            }
            composable(
                route = "${Screen.ReplacePolicyRoute.route}/{${Screen.ReplacePolicyRoute.REPLACE_POLICY_ACTION_ARG}}"
            ) { backStackEntry ->
                ReplacePolicyScreen(
                    navController = navController,
                    policySetupAction = backStackEntry.arguments?.getString(Screen.ReplacePolicyRoute.REPLACE_POLICY_ACTION_ARG)
                        ?.let { PolicySetupAction.valueOf(it) } ?: PolicySetupAction.AddApprovers
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
            composable(
                route = Screen.LoginIdResetRoute.route,
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$OWNER_LOGIN_ID_RESET_URI{$DL_RESET_TOKEN_KEY}"
                    }
                )
            ) {backStackEntry ->
                val resetToken = backStackEntry.arguments?.getString(DL_RESET_TOKEN_KEY)
                LoginIdResetScreen(resetToken, navController = navController)
            }
            composable(
                "$CENSO_IMPORT_DEEPLINK?$IMPORT_KEY_KEY={$IMPORT_KEY_KEY}?$TIMESTAMP_KEY={$TIMESTAMP_KEY}?$SIGNATURE_KEY={$SIGNATURE_KEY}?$NAME_KEY={$NAME_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern =
                            "${DeepLinkURI.CENSO_IMPORT_URI}{$IMPORT_KEY_KEY}/{$TIMESTAMP_KEY}/{$SIGNATURE_KEY}/{$NAME_KEY}"
                    }
                )
            ) { backStackEntry ->
                val importKey = backStackEntry.arguments?.getString(IMPORT_KEY_KEY) ?: ""
                val timestampKey = backStackEntry.arguments?.getString(TIMESTAMP_KEY) ?: ""
                val signatureKey = backStackEntry.arguments?.getString(SIGNATURE_KEY) ?: ""
                val nameKey = backStackEntry.arguments?.getString(NAME_KEY) ?: ""

                PhraseImportScreen(
                    navController = navController,
                    import = Import.fromDeeplink(
                        importKey = importKey,
                        timestamp = timestampKey,
                        signature = signatureKey,
                        name = nameKey
                    )
                )
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