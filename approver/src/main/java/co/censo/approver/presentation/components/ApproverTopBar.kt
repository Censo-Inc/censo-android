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
import androidx.compose.ui.tooling.preview.Preview
import co.censo.approver.R
import co.censo.approver.presentation.ApproverColors
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverTopBar(
    onClose: () -> Unit
) {

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = ApproverColors.NavBar,
            navigationIconContentColor = Color.Black,
            titleContentColor = Color.Black,
        ),
        navigationIcon = {
            IconButton(
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = SharedColors.MainIconColor
                )
            }
        },
        title = {
            // Titles are part of the screen content
        },
    )
}

@Preview
@Composable
fun PreviewTopBar() {
    ApproverTopBar {

    }
}