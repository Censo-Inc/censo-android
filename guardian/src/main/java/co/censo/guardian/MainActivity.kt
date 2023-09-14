package co.censo.guardian

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import ParticipantId
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import co.censo.guardian.presentation.Screen
import co.censo.guardian.presentation.Screen.Companion.DL_DEVICE_PUBLIC_KEY_KEY
import co.censo.guardian.presentation.Screen.Companion.DL_INTERMEDIATE_KEY_KEY
import co.censo.guardian.presentation.Screen.Companion.DL_PARTICIPANT_ID_KEY
import co.censo.guardian.presentation.Screen.Companion.GUARDIAN_DEEPLINK_ACCEPTANCE
import co.censo.guardian.presentation.Screen.Companion.GUARDIAN_URI
import co.censo.guardian.presentation.guardian_entrance.GuardianEntranceArgs
import co.censo.guardian.presentation.guardian_entrance.GuardianEntranceScreen
import co.censo.guardian.presentation.home.HomeScreen
import co.censo.guardian.ui.theme.GuardianTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            GuardianTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CensoNavHost(navController = navController)
                }
            }
        }
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        NavHost(navController = navController, startDestination = Screen.HomeRoute.route) {
            composable(
                "$GUARDIAN_DEEPLINK_ACCEPTANCE?$DL_INTERMEDIATE_KEY_KEY={$DL_INTERMEDIATE_KEY_KEY}?$DL_DEVICE_PUBLIC_KEY_KEY={$DL_DEVICE_PUBLIC_KEY_KEY}?$DL_PARTICIPANT_ID_KEY={$DL_PARTICIPANT_ID_KEY}",
                deepLinks = listOf(navDeepLink {
                    uriPattern =
                        "$GUARDIAN_URI{$DL_INTERMEDIATE_KEY_KEY}/{$DL_DEVICE_PUBLIC_KEY_KEY}/{$DL_PARTICIPANT_ID_KEY}"
                }),
            ) { backStackEntry ->
                val intermediateKey = backStackEntry.arguments?.getString(
                    DL_INTERMEDIATE_KEY_KEY
                )
                val ownerDevicePublicKey = backStackEntry.arguments?.getString(
                    DL_DEVICE_PUBLIC_KEY_KEY
                )
                val participantId = backStackEntry.arguments?.getString(DL_PARTICIPANT_ID_KEY)

                val args = GuardianEntranceArgs(
                    participantId = ParticipantId(participantId ?: ""),
                    ownerDevicePublicKey = Base58EncodedDevicePublicKey(ownerDevicePublicKey ?: ""),
                    intermediateKey = Base58EncodedIntermediatePublicKey(intermediateKey ?: "")
                )

                GuardianEntranceScreen(args = args)
            }
            composable(
                Screen.HomeRoute.route
            ) {
                HomeScreen(navController = navController)
            }
        }
    }
}