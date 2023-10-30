package co.censo.guardian.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import co.censo.guardian.R
import co.censo.guardian.data.ApproverOnboardingUIState
import co.censo.guardian.data.ApproverUIState
import co.censo.guardian.presentation.GuardianColors
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.data.model.Guardian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianTopBar(
    uiState: GuardianUIState,
    onClose: () -> Unit
) {

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = GuardianColors.NavBar,
            navigationIconContentColor = Color.Black,
            titleContentColor = Color.Black,
        ),
        navigationIcon = {
            if (isClosable(uiState)) {
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        stringResource(R.string.close),
                    )
                }
            }
        },
        title = {
            // Titles are part of the screen content
        },
    )
}

fun isClosable(uiState: GuardianUIState): Boolean {
    return when (uiState) {
        GuardianUIState.MISSING_INVITE_CODE,
        GuardianUIState.COMPLETE,
        GuardianUIState.ACCESS_APPROVED -> false

        else -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverTopBar(
    uiState: ApproverUIState,
    onClose: () -> Unit
) {

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = GuardianColors.NavBar,
            navigationIconContentColor = Color.Black,
            titleContentColor = Color.Black,
        ),
        navigationIcon = {
            if (approverTopAppBarIsClosable(uiState)) {
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        stringResource(R.string.close),
                    )
                }
            }
        },
        title = {
            // Titles are part of the screen content
        },
    )
}

fun approverTopAppBarIsClosable(uiState: ApproverUIState): Boolean {
    return when (uiState) {
        ApproverOnboardingUIState.Complete,
        ApproverOnboardingUIState.MissingInviteCode -> false
        //TODO: Setup access Approved here once we finish ApproverAccessVM

        else -> true
    }
}

@Preview
@Composable
fun GuardianTopBarPreview() {
    GuardianTopBar(
        uiState = GuardianUIState.WAITING_FOR_CODE,
        onClose = {}
    )
}