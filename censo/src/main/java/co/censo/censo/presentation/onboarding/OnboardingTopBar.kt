package co.censo.censo.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun OnboardingTopBar(
    onCancel: () -> Unit,
    title: String = "",
    onboarding: Boolean = true,
) {

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
            if (onboarding) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.exit),
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun OnboardingBarPreview() {
    OnboardingTopBar(
        title = "Terms of Use",
        onboarding = true,
        onCancel = {}
    )
}

@Preview
@Composable
fun NotOnboardingTopBarPreview() {
    OnboardingTopBar(
        title = "Terms of Use",
        onboarding = false,
        onCancel = {}
    )
}
