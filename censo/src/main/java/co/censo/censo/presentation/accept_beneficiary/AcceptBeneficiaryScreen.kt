package co.censo.censo.presentation.accept_beneficiary

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.components.DeleteBeneficiaryUserConfirmationUI
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.initial_plan_setup.ScanFaceInformationUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.ClipboardHelper

@Composable
fun AcceptBeneficiaryInvitationScreen(
    inviteId: String?,
    navController: NavController,
    viewModel: AcceptBeneficiaryInvitationViewModel = hiltViewModel()
) {

    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(key1 = state) {
        if (state.navigateToBeneficiary is Resource.Success) {
            navController.navigate(Screen.Beneficiary.route)
            viewModel.resetNavigation()
        }

        if (state.navigateToSignIn is Resource.Success) {
            Toast.makeText(
                context,
                context.getString(R.string.need_to_sign_in_to_continue),
                Toast.LENGTH_LONG
            ).show()
            navController.navigate(Screen.BeneficiarySignInRoute.buildBeneficiaryNavRoute(state.invitationId))
            viewModel.resetNavigation()
        }

        if (state.badLinkPasted is Resource.Error) {
            Toast.makeText(
                context,
                context.getString(R.string.invalid_link_pasted_please_try_again),
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetLinkMessage()
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart(inviteId)
            else -> Unit
        }
    }

    when (state.acceptBeneficiaryUIState) {
        AcceptBeneficiaryUIState.Welcome -> {
            val title = if (state.userLoggedIn) stringResource(R.string.becoming_a_beneficiary) else stringResource(R.string.welcome_to_censo)
            val message = if (state.userLoggedIn) stringResource(R.string.re_enter_beneficiary_message) else stringResource(R.string.standard_beneficiary_message)
            val buttonText = if (state.userLoggedIn) stringResource(id = R.string.paste_from_clipboard) else stringResource(id = R.string.continue_text)
            Box(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                WelcomeBeneficiaryScreen(
                    title = title,
                    message = message,
                    buttonText = buttonText,
                    onContinue = {
                        val pastedInfo = if (state.userLoggedIn) {
                            ClipboardHelper.getClipboardContent(context)
                        } else ""
                        viewModel.onContinue(pastedInfo)
                    }
                )
            }
        }

        AcceptBeneficiaryUIState.FacetecInfo -> {
            ScanFaceInformationUI(
                beginFaceScan = viewModel::startFacetecScan,
                isInfoViewVisible = false,
                showInfoView = {

                }
            )
        }
    }

    if (state.facetecInProgress) {
        FacetecAuth(
            onFaceScanReady = viewModel::onFaceScanReady,
            onCancelled = viewModel::stopFacetecScan
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            onClick = viewModel::showDeleteUserDialog
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close),
                tint = SharedColors.MainIconColor
            )
        }
    }

    if (state.triggerDeleteUserDialog is Resource.Success) {
        DeleteBeneficiaryUserConfirmationUI(
            title = stringResource(id = R.string.delete_user),
            onCancel = viewModel::onCancelResetUser,
            onDelete = viewModel::deleteUser,
        )
    }
    
    if (state.deleteUserResource is Resource.Loading) {
        LargeLoading(fullscreen = true)
    }
}