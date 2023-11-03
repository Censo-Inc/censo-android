package co.censo.censo.presentation.access_seed_phrases.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.access_seed_phrases.AccessPhrasesUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessPhrasesTopBar(
    accessPhrasesUIState: AccessPhrasesUIState,
    phraseLabel: String? = null,
    onNavClicked: () -> Unit
) {

    val title = when (accessPhrasesUIState) {
        AccessPhrasesUIState.ReadyToStart,
        AccessPhrasesUIState.SelectPhrase -> stringResource(id = R.string.access)
        AccessPhrasesUIState.Facetec -> ""
        AccessPhrasesUIState.ViewPhrase -> phraseLabel ?: stringResource(id = R.string.access)
    }

    val navIcon = when (accessPhrasesUIState) {
        AccessPhrasesUIState.SelectPhrase -> Icons.Rounded.ArrowBack to stringResource(id = R.string.back)
        AccessPhrasesUIState.Facetec -> null
        AccessPhrasesUIState.ReadyToStart,
        AccessPhrasesUIState.ViewPhrase -> Icons.Rounded.Close to stringResource(id = R.string.exit)
    }

    Column {


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
            navigationIcon = {
                navIcon?.let {
                    IconButton(onClick = onNavClicked) {
                        Icon(
                            imageVector = it.first,
                            contentDescription = it.second
                        )
                    }
                }
            }
        )
        Divider()
    }
}