package co.censo.vault.presentation.access_seed_phrases

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.facetec_auth.FacetecAuth
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AccessSeedPhrasesScreen(
    navController: NavController,
    viewModel: AccessSeedPhrasesScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
            }
        }
    }

    when {
        state.loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = VaultColors.PrimaryColor)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.White
                )
            }
        }

        state.asyncError -> {
            when {
                state.retrieveShardsResponse is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.retrieveShardsResponse.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.reset() }
                }
            }
        }

        else -> {

            when (state.retrieveShardsResponse) {
                Resource.Uninitialized -> {
                    FacetecAuth(
                        onFaceScanReady = { verificationId, biometry ->
                            viewModel.onFaceScanReady(
                                verificationId,
                                biometry
                            )
                        }
                    )
                }

                is Resource.Success -> {
                    when (val recoveredPhrases = state.recoveredPhrases) {
                        is Resource.Success -> {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "Your Seed Phrases",
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    fontSize = 36.sp
                                )

                                Spacer(Modifier.height(12.dp))

                                LazyColumn(
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    val phrases = recoveredPhrases.data!!

                                    items(phrases.size) { index ->
                                        Column {
                                            Text(
                                                text = "${stringResource(R.string.added_on)}: ${phrases[index].createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE)}", color = Color.Gray, fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Label: ${phrases[index].label}", color = Color.Gray, fontSize = 18.sp
                                            )
                                            Text(
                                                text = phrases[index].seedPhrase,
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                overflow = TextOverflow.Visible,
                                                modifier = Modifier.wrapContentWidth(),
                                            )

                                            Spacer(Modifier.height(12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        is Resource.Error -> {

                        }

                        is Resource.Loading -> {
                            // decrypting
                        }

                        else -> {

                        }
                    }


                }

                else -> {}
            }
        }
    }
}
