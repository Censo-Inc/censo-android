package co.censo.approver

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
import co.censo.approver.presentation.Screen
import co.censo.approver.presentation.Screen.Companion.DL_INVITATION_ID_KEY
import co.censo.approver.presentation.Screen.Companion.DL_PARTICIPANT_ID_KEY
import co.censo.approver.presentation.Screen.Companion.APPROVER_DEEPLINK_INVITATION
import co.censo.approver.presentation.Screen.Companion.APPROVER_DEEPLINK_ACCESS
import co.censo.approver.presentation.Screen.Companion.DL_APPROVAL_ID_KEY
import co.censo.approver.presentation.entrance.ApproverEntranceScreen
import co.censo.approver.presentation.home.ApproverAccessScreen
import co.censo.approver.presentation.onboarding.ApproverOnboardingScreen
import co.censo.approver.ui.theme.ApproverTheme
import co.censo.shared.DeepLinkURI.APPROVER_INVITE_URI
import co.censo.shared.DeepLinkURI.APPROVER_ACCESS_URI
import co.censo.shared.DeepLinkURI.APPROVER_ACCESS_V2_URI
import co.censo.shared.util.StrongboxUI
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            ApproverTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CensoNavHost(navController = navController)
                }
                StrongboxUI()
            }
        }
    }

    @Composable
    private fun CensoNavHost(navController: NavHostController) {
        NavHost(navController = navController, startDestination = Screen.ApproverEntranceRoute.route) {
            composable(route = Screen.ApproverEntranceRoute.route) {
                ApproverEntranceScreen(
                    navController = navController
                )
            }
            composable(
                Screen.ApproverAccessScreen.route
            ) {
                ApproverAccessScreen(navController = navController)
            }
            composable(
                route = Screen.ApproverOnboardingScreen.route
            ) {
                ApproverOnboardingScreen(navController = navController)
            }
            composable(
                "$APPROVER_DEEPLINK_INVITATION?$DL_INVITATION_ID_KEY={$DL_INVITATION_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$APPROVER_INVITE_URI{$DL_INVITATION_ID_KEY}"
                    }

                )
            ) { backStackEntry ->
                val invitationId = backStackEntry.arguments?.getString(DL_INVITATION_ID_KEY) ?: ""
                ApproverEntranceScreen(
                    navController = navController,
                    invitationId = invitationId
                )
            }
            composable(
                "$APPROVER_DEEPLINK_ACCESS?$DL_PARTICIPANT_ID_KEY={$DL_PARTICIPANT_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$APPROVER_ACCESS_URI{$DL_PARTICIPANT_ID_KEY}"
                    }
                )
            ) { backStackEntry ->
                val participantId = backStackEntry.arguments?.getString(DL_PARTICIPANT_ID_KEY) ?: ""
                ApproverEntranceScreen(
                    navController = navController,
                    accessParticipantId = participantId
                )
            }
            composable(
                "$APPROVER_DEEPLINK_ACCESS?$DL_PARTICIPANT_ID_KEY={$DL_PARTICIPANT_ID_KEY}?$DL_APPROVAL_ID_KEY={$DL_APPROVAL_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$APPROVER_ACCESS_V2_URI{$DL_PARTICIPANT_ID_KEY}/{$DL_APPROVAL_ID_KEY}"
                    }
                )
            ) { backStackEntry ->
                val participantId = backStackEntry.arguments?.getString(DL_PARTICIPANT_ID_KEY) ?: ""
                val approvalId = backStackEntry.arguments?.getString(DL_APPROVAL_ID_KEY) ?: ""
                ApproverEntranceScreen(
                    navController = navController,
                    accessParticipantId = participantId,
                    approvalId = approvalId,
                )
            }
        }
    }
}