package co.censo.approver.presentation.components

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
import co.censo.approver.R
import co.censo.approver.presentation.GuardianColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverTopBar(
    onClose: () -> Unit
) {

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = GuardianColors.NavBar,
            navigationIconContentColor = Color.Black,
            titleContentColor = Color.Black,
        ),
        navigationIcon = {
            IconButton(
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    stringResource(R.string.close),
                )
            }
        },
        title = {
            // Titles are part of the screen content
        },
    )
}