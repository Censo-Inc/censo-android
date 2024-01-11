package co.censo.censo.presentation.import_phrase

import Base64EncodedData
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Import
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import java.util.Base64

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
            Lifecycle.Event.ON_START -> {
                viewModel.onStart(import)
            }
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
                    dismissAction = viewModel::exitFlow,
                    retryAction = { viewModel.acceptImport(import) }
                )
            } else if (state.importErrorType != ImportErrorType.NONE) {
                DisplayError(
                    errorMessage = state.importErrorType.getErrorMessage(context),
                    dismissAction = viewModel::exitFlow,
                    retryAction = null
                )
            } else if (state.userResponse is Resource.Error) {
                DisplayError(
                    errorMessage = stringResource(R.string.unable_get_user_info),
                    dismissAction = viewModel::exitFlow,
                    retryAction = {
                        viewModel.kickOffPhraseImport(import)
                    }
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

                val decodedName = try {
                    val decodedURL = Base64.getUrlDecoder().decode(name)
                    val intoBase64 =
                        Base64EncodedData(Base64.getEncoder().encodeToString(decodedURL))
                    String(intoBase64.bytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    "Samuel"
                }

                Text(
                    stringResource(R.string.user_would_like_to_export_seed, decodedName),
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
                        stringResource(R.string.accept),
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
                        stringResource(id = R.string.decline),
                        style = ButtonTextStyle,
                    )
                }
            }

            ImportPhase.Accepting,
            is ImportPhase.Completing -> {
                Text(
                    text = stringResource(R.string.importing_phrase),
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(36.dp))
                LargeLoading(fullscreen = false)
            }

            is ImportPhase.Completed -> {
                Text(
                    text = stringResource(R.string.phrase_imported),
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
