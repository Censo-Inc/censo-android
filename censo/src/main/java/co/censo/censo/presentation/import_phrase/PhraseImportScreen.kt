package co.censo.censo.presentation.import_phrase

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Import
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@Composable
fun PhraseImportScreen(
    navController: NavController,
    import: Import,
    viewModel: PhraseImportViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {

        if (state.exitFlow is Resource.Success) {
            navController.navigate(Screen.EntranceRoute.route)
            viewModel.resetExitFlow()
        }

        if (state.sendToSeedVerification is Resource.Success) {
            navController.navigate(
                route = state.sendToSeedVerification.data ?: Screen.EntranceRoute.route
            )
            viewModel.resetSendVerification()
        }
    }

    when {
        state.error -> {
            if (state.acceptImportResource is Resource.Error) {
                DisplayError(
                    errorMessage = state.acceptImportResource.getErrorMessage(context),
                    dismissAction = viewModel::exitFlow,
                    retryAction = { viewModel.kickOffPhraseImport(import) }
                )
            } else if (state.getEncryptedResponse is Resource.Error) {
                DisplayError(
                    errorMessage = state.getEncryptedResponse.getErrorMessage(context),
                    dismissAction = viewModel::resetGetEncryptedResponse,
                    retryAction = { viewModel.acceptImport(import) }
                )
            }
        }
        else -> {
            PhraseImportUI(
                name = import.name,
                onAccept = { viewModel.kickOffPhraseImport(import) },
                onDecline = viewModel::exitFlow,
                phraseImportState = state.importPhraseState
            )
        }
    }
}

@Composable
fun PhraseImportUI(
    phraseImportState: ImportPhase,
    name: String, onAccept: () -> Unit, onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        when (phraseImportState) {
            ImportPhase.None -> {
                Text(
                    "$name would like to export a seed phrase to you.",
                    fontSize = 24.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(36.dp))

                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = onAccept
                ) {
                    Text(
                        "Accept",
                        style = ButtonTextStyle,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = onDecline
                ) {
                    Text(
                        "Decline",
                        style = ButtonTextStyle,
                    )
                }
            }

            ImportPhase.Accepting,
            is ImportPhase.Completing -> {
                Text(
                    text = "Importing Phrase...",
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(36.dp))
                LargeLoading(fullscreen = false)
            }

            is ImportPhase.Completed -> {
                Text(
                    text = "Phrase Imported!",
                    fontSize = 24.sp,
                    color = Color.Black
                )
            }
        }

    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewPhraseImportUI() {
    PhraseImportUI(
        name = "Jason",
        onAccept = {},
        onDecline = {},
        phraseImportState = ImportPhase.None
    )
}
