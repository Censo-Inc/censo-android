package co.censo.censo.presentation.main

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(bottomNavItem: BottomNavItem) {

    val title = when (bottomNavItem) {
        BottomNavItem.Approvers -> stringResource(R.string.approvers_app_bar_title)
        BottomNavItem.Home -> ""
        BottomNavItem.Phrases -> stringResource(R.string.seed_phrases_app_bar_title)
        BottomNavItem.Settings -> stringResource(R.string.settings_app_bar_title)
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = VaultColors.NavbarColor,
            navigationIconContentColor = Color.Black,
            titleContentColor = Color.Black,
        ),
        title = {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.W400,
                )
            }
        },
    )
}