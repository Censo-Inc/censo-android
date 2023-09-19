package co.censo.vault.presentation.bip_39_detail

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.util.TestTag
import co.censo.vault.presentation.components.IndexedPhraseWord
import co.censo.vault.presentation.components.PhraseUICompanion
import co.censo.vault.presentation.components.PhraseWords

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BIP39DetailScreen(
    bip39Name: String,
    navController: NavController, viewModel: BIP39DetailViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(bip39Name)
        onDispose {
            viewModel.reset()
        }
    }

    Scaffold(
        modifier = Modifier.semantics { testTag = TestTag.bip_39_detail_screen_container },
        topBar = {
            TopAppBar(
                modifier = Modifier.semantics { testTag = TestTag.bip_39_detail_screen_app_bar },
                title = { Text(text = stringResource(R.string.bip39_detail)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.leave_screen),
                            tint = Color.Black
                        )
                    }
                },
            )
        },
        content = {
            val index = state.currentWordIndex
            val phrase = state.bip39Phrase
            val phraseSize = state.phraseWordCount

            val splitWords = state.bip39Phrase.split(" ")
            val wordsToShow = mutableListOf<IndexedPhraseWord>()

            if (splitWords.size >= index + PhraseUICompanion.DISPLAY_RANGE_SET) {
                for ((wordIndex, word) in splitWords.withIndex()) {
                    if (wordIndex in index..index + PhraseUICompanion.DISPLAY_RANGE_SET) {
                        wordsToShow.add(
                            IndexedPhraseWord(
                                wordIndex = wordIndex + PhraseUICompanion.OFFSET_INDEX_ZERO,
                                wordValue = word
                            )
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //region Title + Sub-title
                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )
                    if (phrase.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                id = R.string.showing_of_words,
                                (index + 1).toString(),
                                (index + 1 + PhraseUICompanion.DISPLAY_RANGE_SET).toString(),
                                phraseSize
                            ),
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(48.dp)
                    )
                    //endregion

                    //Phrase words
                    PhraseWords(phraseWords = wordsToShow)

                    //region Buttons
                    Spacer(
                        modifier = Modifier.height(15.dp)
                    )

                }

                if (phrase.isNotEmpty()) {

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        //Buttons for displaying previous/next words
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.wordIndexChanged(false)
                                    }
                                    .padding(end = 16.dp)
                            ) {
                                Icon(
                                    modifier = Modifier.size(32.dp),
                                    imageVector = Icons.Filled.NavigateBefore,
                                    contentDescription = stringResource(R.string.previous_icon_content_desc),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = stringResource(R.string.previous),
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.wordIndexChanged(true)
                                    }
                                    .padding(start = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.next),
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Icon(
                                    modifier = Modifier.size(32.dp),
                                    imageVector = Icons.Filled.NavigateNext,
                                    contentDescription = stringResource(R.string.next_icon_content_desc),
                                    tint = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        //endregion
                    }
                }
            }
        }
    )

}