package co.censo.approver.presentation.owners

import MessageText
import ParticipantId
import StandardButton
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

const val OWNER_LABEL_MAX_LENGTH = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelOwner(
    navController: NavController,
    participantId: ParticipantId,
    viewModel: LabelOwnerViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart(participantId)
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.saveResource is Resource.Success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            if (navController.previousBackStackEntry?.destination?.route == Screen.ApproverOwnersListScreen.route) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(id = R.string.close),
                            )
                        }
                    },
                    title = { }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.loading -> {
                    LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
                }

                state.asyncError -> {
                    if (state.labelResource is Resource.Error) {
                        DisplayError(
                            errorMessage = state.labelResource.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = { viewModel.onStart(participantId) }
                        )
                    } else if (state.saveResource is Resource.Error) {
                        DisplayError(
                            errorMessage = state.saveResource.getErrorMessage(context),
                            dismissAction = viewModel::resetSaveResource,
                            retryAction = viewModel::save
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 36.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {

                        TitleText(
                            modifier = Modifier.fillMaxWidth(),
                            title = R.string.label_owner_screen_title,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(48.dp))
                        MessageText(
                            modifier = Modifier.fillMaxWidth(),
                            message = R.string.label_owner_screen_text,
                            textAlign = TextAlign.Start
                        )

                        val textFieldStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W500,
                            color = SharedColors.MainColorText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.labelResource.success()?.data ?: "",
                            onValueChange = viewModel::onLabelChanged,
                            shape = CircleShape,
                            placeholder = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.enter_a_nickname),
                                    fontSize = 24.sp,
                                    fontWeight = textFieldStyle.fontWeight,
                                    textAlign = TextAlign.Center,
                                    color = SharedColors.PlaceholderTextGrey,
                                )
                            },
                            textStyle = textFieldStyle,
                            enabled = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = SharedColors.BorderGrey,
                                unfocusedBorderColor = SharedColors.BorderGrey
                            )
                        )

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (state.labelIsTooLong) stringResource(R.string.input_string_is_too_long, OWNER_LABEL_MAX_LENGTH) else " ",
                            textAlign = TextAlign.Center,
                            color = SharedColors.ErrorRed
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        StandardButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.saveEnabled && state.saveResource !is Resource.Loading,
                            onClick = viewModel::save,
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
                        ) {
                            val saveButtonTextStyle = if (state.saveEnabled) ButtonTextStyle else DisabledButtonTextStyle
                            Text(
                                text = stringResource(id = R.string.label_owner_screen_save_button),
                                style = saveButtonTextStyle.copy(fontSize = 20.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}