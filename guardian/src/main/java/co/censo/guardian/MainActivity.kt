package co.censo.guardian

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import co.censo.guardian.presentation.Screen
import co.censo.guardian.presentation.home.GuardianHomeScreen
import co.censo.guardian.presentation.routing.ApproverRoutingScreen
import co.censo.guardian.ui.theme.GuardianTheme
import co.censo.shared.SharedScreen
import co.censo.shared.SharedScreen.Companion.DL_INVITATION_ID_KEY
import co.censo.shared.SharedScreen.Companion.DL_PARTICIPANT_ID_KEY
import co.censo.shared.SharedScreen.Companion.GUARDIAN_DEEPLINK_ACCEPTANCE
import co.censo.shared.SharedScreen.Companion.GUARDIAN_DEEPLINK_RECOVERY
import co.censo.shared.SharedScreen.Companion.GUARDIAN_ONBOARDING_URI
import co.censo.shared.SharedScreen.Companion.GUARDIAN_RECOVERY_URI
import co.censo.shared.presentation.entrance.EntranceScreen
import dagger.hilt.android.AndroidEntryPoint

public val routingLogTag = "ROUTING"

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            GuardianTheme {
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
        NavHost(navController = navController, startDestination = SharedScreen.EntranceRoute.route) {
            composable(route = SharedScreen.EntranceRoute.route) {
                EntranceScreen(
                    navController = navController,
                    guardianEntrance = true
                )
            }
            composable(
                SharedScreen.HomeRoute.route
            ) {
                GuardianHomeScreen(navController = navController)
            }
            composable(
                Screen.ApproverRoutingScreen.route
            ) {
                ApproverRoutingScreen(navController = navController)
            }
            composable(
                "$GUARDIAN_DEEPLINK_ACCEPTANCE?$DL_INVITATION_ID_KEY={$DL_INVITATION_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$GUARDIAN_ONBOARDING_URI{$DL_INVITATION_ID_KEY}"
                    }

                )
            ) { backStackEntry ->
                val invitationId = backStackEntry.arguments?.getString(DL_INVITATION_ID_KEY) ?: ""
                EntranceScreen(
                    navController = navController,
                    invitationId = invitationId,
                    guardianEntrance = true
                )
            }
            composable(
                "$GUARDIAN_DEEPLINK_RECOVERY?$DL_PARTICIPANT_ID_KEY={$DL_PARTICIPANT_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$GUARDIAN_RECOVERY_URI{$DL_PARTICIPANT_ID_KEY}"
                    }
                )
            ) { backStackEntry ->
                val participantId = backStackEntry.arguments?.getString(DL_PARTICIPANT_ID_KEY) ?: ""
                EntranceScreen(
                    navController = navController,
                    recoveryParticipantId = participantId,
                    guardianEntrance = true
                )
            }
        }
    }
}