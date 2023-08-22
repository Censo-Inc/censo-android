package co.censo.vault.presentation.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.util.TestTag

const val TAG = "Vault51"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController, viewModel: HomeViewModel = hiltViewModel()
) {

    val state = viewModel.state

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    Scaffold(
        modifier = Modifier.semantics {
            testTag = TestTag.home_screen_container
        },
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = TestTag.home_screen_app_bar },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text = stringResource(R.string.your_bip39_phrases))
                        
                        IconButton(onClick = {
                            //TODO: Navigate to screen for guardian invitation
                            Log.i(TAG, "HomeScreen: Guardian Invitation Icon clicked on Home screen")
                            navController.navigate(Screen.GuardianInvitationRoute.route)
                        }) {
                            Icon(imageVector = Icons.Default.PersonAddAlt1, contentDescription = "")
                        }
                    }
                })
        },
        content = {
            Box(modifier = Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .semantics { testTag = TestTag.phrases_list },
                ) {
                    state.phrases.forEach { phrase ->
                        ClickableText(
                            modifier = Modifier.padding(all = 48.dp),
                            text = AnnotatedString(text = phrase.key),
                            onClick = {
                                navController.navigate(
                                    "${Screen.BIP39DetailRoute.route}/${phrase.key}"
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.semantics { testTag = TestTag.add_bip39_button },
                onClick = {
                    navController.navigate(Screen.AddBIP39Route.route)
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_bip39_phrase),
                    tint = Color.White,
                )
            }
        })
}