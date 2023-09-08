package co.censo.vault.presentation.guardian_entrance

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun GuardianEntranceScreen(
    navController: NavController,
    args: GuardianEntranceArgs,
    viewModel: GuardianEntranceViewModel = hiltViewModel()
) {
    Text(text = "Guardian Entrance")

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(args)
        onDispose {  }
    }


}