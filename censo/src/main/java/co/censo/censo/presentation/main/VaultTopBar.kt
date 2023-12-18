package co.censo.censo.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    bottomNavItem: BottomNavItem,
    showCloseApprover: Boolean,
    onDismissApprover: () -> Unit
) {

    val title = when (bottomNavItem) {
        BottomNavItem.Phrases -> stringResource(R.string.seed_phrases_app_bar_title)
        BottomNavItem.Settings,
        BottomNavItem.Home -> ""
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            navigationIconContentColor = SharedColors.MainIconColor,
            titleContentColor = SharedColors.MainColorText,
        ),
        title = {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    color = SharedColors.MainColorText,
                    fontWeight = FontWeight.W400,
                )
            }
        },
        navigationIcon = {
            if (showCloseApprover) {
                IconButton(onClick = onDismissApprover) {
                    Icon(
                        modifier = Modifier.size(72.dp),
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.exit),
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun TopBarPreview() {
    VaultTopBar(
        bottomNavItem = BottomNavItem.Phrases,
        showCloseApprover = false,
        onDismissApprover = {}
    )
}

@Preview
@Composable
fun EmptyTopBarPreview() {
    VaultTopBar(
        bottomNavItem = BottomNavItem.Settings,
        showCloseApprover = false,
        onDismissApprover = {}
    )
}

@Preview
@Composable
fun CloseApproversScreenBarPreview() {
    VaultTopBar(
        bottomNavItem = BottomNavItem.Home,
        showCloseApprover = true,
        onDismissApprover = { }
    )
}