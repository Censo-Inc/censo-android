package co.censo.vault.presentation.owner_entrance

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.data.Resource
import co.censo.vault.presentation.components.owner_information.OwnerInformationField
import co.censo.vault.presentation.components.owner_information.OwnerInformationRow
import co.censo.vault.presentation.components.owner_information.VerifyCode
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.BiometricUtil

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerEntranceScreen(
    navController: NavController,
    viewModel: OwnerEntranceViewModel = hiltViewModel()
) {

    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.bioPromptTrigger is Resource.Success) {
            val promptInfo = BiometricUtil.createPromptInfo(context)

            val bioPrompt = BiometricUtil.createBioPrompt(
                fragmentActivity = context,
                onSuccess = {
                    viewModel.onBiometryApproved()
                },
                onFail = {
                    BiometricUtil.handleBioPromptOnFail(context = context, errorCode = it) {
                        viewModel.onBiometryFailed()
                    }
                }
            )

            bioPrompt.authenticate(promptInfo)
        }

        if (state.userFinishedSetup is Resource.Success) {
            state.userFinishedSetup.data?.let {
                navController.navigate(it)
            }
            viewModel.resetUserFinishedSetup()
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth(),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.owner_setup))
                    }
                })
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color = Color.White),
            ) {

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.White)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(44.dp))
                        }
                    }

                    state.apiCallErrorOccurred -> {
                        if (state.createOwnerResource is Resource.Error) {
                            DisplayError(
                                errorMessage = state.createOwnerResource.getErrorMessage(context)
                            ) {
                                //TODO: Retry when create owner fails
                            }
                        } else if (state.verificationResource is Resource.Error) {
                            DisplayError(
                                errorMessage = state.verificationResource.getErrorMessage(context)
                            ) {
                                //TODO: Retry when verify resource fails
                            }
                        } else if (state.userResource is Resource.Error) {
                            DisplayError(
                                errorMessage = state.userResource.getErrorMessage(context)
                            ) {
                                //TODO: Retry when get user
                            }
                        }
                    }

                    else -> {
                        OwnerEntranceStandardUI(
                            state = state,
                            updateContact = viewModel::updateContactValue,
                            updateVerificationCode = viewModel::updateVerificationCode,
                            showVerificationDialog = viewModel::showVerificationDialog,
                            ownerAction = viewModel::ownerAction
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun OwnerEntranceStandardUI(
    state: OwnerEntranceState,
    updateContact: (String) -> Unit,
    updateVerificationCode: (String) -> Unit,
    showVerificationDialog: () -> Unit,
    ownerAction: (OwnerAction) -> Unit
) {
    when (state.userStatus) {
        UserStatus.CREATE_CONTACT ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = "Owner Information", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (state.emailContactState() == ContactState.DOES_NOT_EXIST) {
                    OwnerInformationField(
                        value = state.contactValue,
                        onValueChange = updateContact,
                        placeholderText = "User Contact",
                        keyboardActions = KeyboardActions(
                            onNext = {
                                ownerAction(OwnerAction.EmailSubmitted)
                            }),
                        error = state.validationError,
                        isLoading = state.isLoading,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

        UserStatus.CONTACT_UNVERIFIED ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = "Owner Information", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OwnerInformationRow(
                    value = "Owner Email: ${state.contactValue}",
                    valueVerified = state.contactVerified,
                    onVerifyClicked = {
                        showVerificationDialog()
                    },
                    onEditClicked = {})
            }

        UserStatus.VERIFY_CODE_ENTRY ->
            VerifyCode(
                value = state.verificationCode,
                onValueChange = { updateVerificationCode(it) },
                onDone = { ownerAction(OwnerAction.EmailVerification) },
                isLoading = false
            )

        UserStatus.COMPLETE_FACETEC, UserStatus.UNINITIALIZED -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
fun DisplayError(
    errorMessage: String,
    retryAction: () -> Unit,
) {
    Text(modifier = Modifier.padding(16.dp), text = errorMessage, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = retryAction) {
        Text(text = stringResource(R.string.retry))
    }
}