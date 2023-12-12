package co.censo.censo.presentation.main

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(bottomNavItem: BottomNavItem) {

    val title = when (bottomNavItem) {
        BottomNavItem.Phrases -> stringResource(R.string.seed_phrases_app_bar_title)
        BottomNavItem.Approvers -> stringResource(R.string.approvers_app_bar_title)
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
    )
}

@Preview
@Composable
fun TopBarPreview() {
    VaultTopBar(bottomNavItem = BottomNavItem.Approvers)
}

@Preview
@Composable
fun EmptyTopBarPreview() {
    VaultTopBar(bottomNavItem = BottomNavItem.Settings)
}