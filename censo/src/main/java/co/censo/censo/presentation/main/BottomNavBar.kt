package co.censo.censo.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R

@Composable
fun CensoBottomNavBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        for (item in bottomNavItems) {
            BottomNavBarItemUI(
                navItem = item,
                selected = selectedItem == item
            ) {
                onItemSelected(item)
            }
        }
    }
}


val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Phrases,
    BottomNavItem.Settings,
)

@Composable
fun RowScope.BottomNavBarItemUI(
    navItem: BottomNavItem,
    selected: Boolean,
    onSelected: () -> Unit
) {
    NavigationBarItem(
        colors = NavigationBarItemDefaults.censoDefaults(),
        selected = selected,
        label = {
            Text(
                text = stringResource(id = navItem.text),
                color = SharedColors.BottomNavBarTextColor
            )
        },
        onClick = onSelected,
        icon = {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(id = navItem.icon),
                contentDescription = navItem.name,
            )
        })
}

@Composable
fun NavigationBarItemDefaults.censoDefaults() =
    colors(
        selectedIconColor = SharedColors.BottomNavBarIconColor,
        unselectedIconColor = SharedColors.BottomNavBarIconColor,
        indicatorColor = SharedColors.BottomNavBarIndicatorColor
    )

enum class BottomNavItem(@StringRes val text: Int, @DrawableRes val icon: Int) {
    Home(
        text = R.string.home_nav_title,
        icon = R.drawable.home_tab_icon
    ),
    Phrases(
        text = R.string.phrases_nav_title,
        icon = R.drawable.lock_tab_icon
    ),
    Settings(
        text = R.string.settings_nav_title,
        icon = R.drawable.settings_tab_icon,
    )
}