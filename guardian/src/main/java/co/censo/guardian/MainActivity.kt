package co.censo.guardian

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import co.censo.guardian.presentation.Screen.Companion.DL_INVITATION_ID_KEY
import co.censo.guardian.presentation.Screen.Companion.GUARDIAN_DEEPLINK_ACCEPTANCE
import co.censo.guardian.presentation.guardian_entrance.GuardianEntranceScreen
import co.censo.guardian.ui.theme.GuardianTheme
import co.censo.shared.data.repository.OwnerRepositoryImpl.Companion.GUARDIAN_URI
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
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
        NavHost(navController = navController, startDestination = Screen.GuardianEntranceRoute.route) {
            composable(
                Screen.GuardianEntranceRoute.route
            ) {
                GuardianEntranceScreen(navController = navController)
            }
            composable(
                "$GUARDIAN_URI?$DL_INVITATION_ID_KEY={$DL_INVITATION_ID_KEY}",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "$GUARDIAN_DEEPLINK_ACCEPTANCE{$DL_INVITATION_ID_KEY}"
                    }
                )
            ) {
                //TODO: Handle Deeplink args
                GuardianEntranceScreen(navController = navController)
            }
        }
    }
}