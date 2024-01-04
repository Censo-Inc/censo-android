package co.censo.approver.presentation.owners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverOwnersList(
    navController: NavController,
    viewModel: ApproverOwnersListViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let { navigationData ->
                navController.navigate(navigationData.route)
            }
            viewModel.resetNavigationResource()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(id = R.string.close),
                        )
                    }
                },
                title = { }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            when {
                state.loading -> {
                    LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
                }

                state.asyncError -> {
                    if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.userResponse.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::retrieveApproverState,
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 36.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        Text(
                            text = stringResource(R.string.owners_list_title),
                            fontSize = 38.sp,
                            color = SharedColors.MainColorText
                        )

                        Spacer(modifier = Modifier.height(48.dp))
                        (state.userResponse.data?.approverStates ?: emptyList()).forEach { approverState ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = SharedColors.BorderGrey,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(
                                        horizontal = 20.dp,
                                        vertical = 20.dp
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = approverState.ownerLabel ?: "-",
                                    color = SharedColors.MainColorText,
                                    fontSize = 24.sp
                                )

                                IconButton(onClick = { viewModel.navToEditLabelScreen(approverState.participantId) }) {
                                    Icon(
                                        painterResource(id = co.censo.shared.R.drawable.edit_icon),
                                        contentDescription = stringResource(R.string.edit_owner_label),
                                        tint = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}