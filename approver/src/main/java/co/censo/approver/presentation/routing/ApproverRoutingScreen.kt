package co.censo.approver.presentation.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.presentation.Screen
import co.censo.approver.presentation.components.PasteLinkHomeScreen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.ClipboardHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverRoutingScreen(
    navController: NavController,
    viewModel: ApproverRoutingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.navToApproverAccess is Resource.Success) {
            navController.navigate(Screen.ApproverAccessScreen.route) {
                popUpTo(Screen.ApproverRoutingScreen.route) {
                    inclusive = true
                }
            }

            viewModel.resetApproverAccessNavigationTrigger()
        }

        if (state.navToApproverOnboarding is Resource.Success) {
            navController.navigate(Screen.ApproverOnboardingScreen.route) {
                popUpTo(Screen.ApproverRoutingScreen.route) {
                    inclusive = true
                }
            }
            viewModel.resetApproverOnboardingNavigationTrigger()
        }
    }


    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { viewModel.onStop() }
    }

    Scaffold(
        content = { contentPadding ->

            when {
                viewModel.state.linkError ->
                    DisplayError(
                        errorMessage = "${stringResource(R.string.link_not_valid)} - (${ClipboardHelper.getClipboardContent(context)})",
                        dismissAction = { viewModel.clearError() },
                        retryAction = null
                    )

                viewModel.state.showPasteLink -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(0.3f))

                            PasteLinkHomeScreen {
                                viewModel.userPastedLink(
                                    ClipboardHelper.getClipboardContent(context)
                                )
                            }

                            Spacer(modifier = Modifier.weight(0.7f))
                        }

                        if (viewModel.state.hasApprovers) {
                            Column(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painterResource(
                                        id = R.drawable.active_approvers_icon
                                    ),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Active approver",
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.W500
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }

                else ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(72.dp)
                                .align(Alignment.Center),
                            strokeWidth = 8.dp,
                            color = Color.Black
                        )
                    }
            }
        }
    )

}
