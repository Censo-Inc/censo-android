package co.censo.censo

import Base58EncodedApproverPublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.censo.shared.data.storage.SecurePreferences
import co.censo.censo.presentation.entrance.OwnerEntranceScreen
import co.censo.censo.presentation.access_seed_phrases.AccessSeedPhrasesScreen
import co.censo.censo.presentation.enter_phrase.EnterPhraseScreen
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.main.MainVaultScreen
import co.censo.censo.presentation.initial_plan_setup.InitialPlanSetupScreen
import co.censo.censo.presentation.lock_screen.LockedScreen
import co.censo.censo.presentation.plan_setup.PolicySetupScreen
import co.censo.censo.presentation.access_approval.AccessApprovalScreen
import co.censo.censo.presentation.main.BottomNavItem
import co.censo.censo.presentation.paywall.PaywallScreen
import co.censo.censo.presentation.plan_finalization.ReplacePolicyScreen
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.ui.theme.VaultTheme
import co.censo.censo.util.TestTag
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.util.StrongboxUI
import co.censo.shared.util.projectLog
import dagger.hilt.android.AndroidEntryPoint
import io.github.novacrypto.base58.Base58
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.util.Base64
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var storage: SecurePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fooTest()
//        setupPushChannel()
//
//        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
//
//        setContent {
//            val navController = rememberNavController()
//
//            VaultTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .semantics {
//                            testTag = TestTag.main_activity_surface_container
//                        },
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Box {
//                        Box(
//                            modifier = Modifier.fillMaxHeight(),
//                        ) {
//                            CensoNavHost(navController = navController)
//                        }
//
//                        Box(
//                            modifier = Modifier.align(Alignment.BottomCenter)
//                        ) {
//                            LockedScreen()
//                        }
//
//                        Box(
//                            modifier = Modifier.fillMaxSize()
//                        ) {
//                            PaywallScreen()
//                        }
//                    }
//
//                    StrongboxUI()
//                }
//            }
//        }
    }

    fun fooTest() {
        //region Recreate the key
        val fooKey = InitialKeyData(encryptedPrivateKey= byteArrayOf(14, 12, -87, -62, 0, -12, 41, 102, 111, 102, -43, -50, -2, 38, 63, 118, -69, -119, 108, -39, 12, -19, -31, 0, 58, -49, 10, 125, 33, 64, -34, -64, -44, 60, -93, 48, 79, 9, 28, -49, 126, -124, 102, -15, -72, 101, 14, -86, -31, -96, -82, 49, 49, 48, 1, -99), publicKey=Base58EncodedApproverPublicKey(value="SGBxgrcqdDrCKRBJDDFXNTrQ3bj8GGojTXBAeWbqv8VtrzaTCsqsBjAr7r4bgPfPGTV8ik4b2pmGkPaLcpbpjNV7"))

        val entropy = Base64EncodedData("uG+zFogeQ/XkUbjKPUgSWlp3IAhfinXCiCAfYDxuIAdwv1JoKENUF2mYHiBCBbfdmsW+dIUPgaVLKduAvVWwaQ==")
        val deviceKeyId = "102236090183538294473"

        val recreatedKey = EncryptionKey.generateFromPrivateKeyRaw(
            fooKey.encryptedPrivateKey.decryptWithEntropy(
                deviceKeyId = deviceKeyId,
                entropy = entropy
            ).bigInt()
        )

        //endregion

        //1. Signature From Backend: Base64EncodedData = MEQCIEaJHF6qlsUxV98b622P6rCIDNdwl29CVO36NgrpbIyoAiBS6NiLFHcY4PdbHhWbF98ERdBvyRq26kR4IYntSnPrAw==
        //2. Key that was signed: Base58EncodedMasterPublicKey = Rumgf4o6YAtBZER8j8wCeAy9DPnMSZeFJvqLQzK6dtiAqai5AiSeL1jEGAWNyWVQjC1HqibMo5F3gs8a4V1VAbmd

        val signature = Base64EncodedData("MEQCIEaJHF6qlsUxV98b622P6rCIDNdwl29CVO36NgrpbIyoAiBS6NiLFHcY4PdbHhWbF98ERdBvyRq26kR4IYntSnPrAw==")
        val signedData = Base58EncodedMasterPublicKey("Rumgf4o6YAtBZER8j8wCeAy9DPnMSZeFJvqLQzK6dtiAqai5AiSeL1jEGAWNyWVQjC1HqibMo5F3gs8a4V1VAbmd")

        val signedDataAsBytes = (signedData.ecPublicKey as BCECPublicKey).q.getEncoded(false)

        val verified = recreatedKey.verify(
            signedData = signedDataAsBytes,
            signature = signature.bytes //This is correct because it is using backend data
        )

        projectLog(message = "Verified: $verified")
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        val bottomNavItem: MutableState<BottomNavItem> = remember { mutableStateOf(BottomNavItem.Home) }

        NavHost(
            navController = navController,
            startDestination = Screen.EntranceRoute.route
        ) {
            composable(route = Screen.EntranceRoute.route) {
                OwnerEntranceScreen(navController = navController)
            }
            composable(route = Screen.OwnerVaultScreen.route) {
                MainVaultScreen(selectedBottomNavItem = bottomNavItem, navController = navController)
            }
            composable(route = "${Screen.AccessApproval.route}/{${Screen.AccessApproval.ACCESS_INTENT_ARG}}") { backStackEntry ->
                AccessApprovalScreen(
                    navController = navController,
                    accessIntent = backStackEntry.arguments?.getString(Screen.AccessApproval.ACCESS_INTENT_ARG)
                        ?.let { AccessIntent.valueOf(it) } ?: AccessIntent.AccessPhrases,
                )
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
                    welcomeFlow = (backStackEntry.arguments?.getString(Screen.EnterPhraseRoute.WELCOME_FLOW_ARG)
                        ?.toBoolean()) ?: false
                )
            }
            composable(
                route = "${Screen.PolicySetupRoute.route}/{${Screen.PolicySetupRoute.SETUP_ACTION_ARG}}"
            ) {backStackEntry ->
                PolicySetupScreen(
                    navController = navController,
                    policySetupAction = backStackEntry.arguments?.getString(Screen.PolicySetupRoute.SETUP_ACTION_ARG)
                        ?.let { PolicySetupAction.valueOf(it) } ?: PolicySetupAction.AddApprovers
                )
            }
            composable(
                route = "${Screen.ReplacePolicyRoute.route}/{${Screen.ReplacePolicyRoute.REPLACE_POLICY_ACTION_ARG}}"
            ) {backStackEntry ->
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