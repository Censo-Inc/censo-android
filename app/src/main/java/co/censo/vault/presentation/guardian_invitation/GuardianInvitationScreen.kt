package co.censo.vault.presentation.guardian_invitation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.data.Resource
import co.censo.vault.presentation.components.owner_information.OwnerInformationField
import co.censo.vault.presentation.components.owner_information.OwnerInformationRow
import co.censo.vault.presentation.components.owner_information.VerifyCode
import co.censo.vault.util.BiometricUtil

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LogNotTimber")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianInvitationScreen(
    navController: NavController, viewModel: GuardianInvitationViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.bioPromptTrigger is Resource.Success) {
            val promptInfo = BiometricUtil.createPromptInfo(context)

            val bioPrompt = BiometricUtil.createBioPrompt(
                fragmentActivity = context,
                onSuccess = {
                    viewModel.onBiometryApproved(state.bioPromptTrigger.data!!)
                },
                onFail = {
                    BiometricUtil.handleBioPromptOnFail(context = context, errorCode = it) {
                        viewModel.onBiometryFailed()
                    }
                }
            )

            bioPrompt.authenticate(promptInfo)
        }

        if (state.showToast is Resource.Success) {
            val message = state.showToast.data?.getErrorMessage(context)
                ?: context.getString(R.string.default_err_message)
            viewModel.resetShowToast()

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    Scaffold(topBar = {
        TopAppBar(modifier = Modifier.fillMaxWidth(), title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Guardian Invitation")
            }
        }, navigationIcon = {
            IconButton(onClick = {
                navController.navigateUp()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back navigation arrow"
                )
            }
        })
    }) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.user is Resource.Loading) {
                CircularProgressIndicator()
            } else {
                when (state.ownerState) {
                    OwnerState.NEW -> {
                        OwnerInformationField(
                            value = state.ownerName,
                            onValueChange = viewModel::updateOwnerName,
                            placeholderText = "Owner Name (you)",
                            keyboardActions = KeyboardActions(onNext = {
                                viewModel.ownerAction(OwnerAction.NameSubmitted)
                            }),
                            isLoading = state.isLoading,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    OwnerState.VERIFYING -> {
                        when (state.ownerInputState) {
                            OwnerInputState.VIEWING_CONTACTS -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(text = "Owner Information", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Owner Name: ${state.ownerName}")

                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (state.emailContactState()) {
                                        ContactState.DOES_NOT_EXIST -> {
                                            OwnerInformationField(
                                                value = state.emailContactStateData.value,
                                                onValueChange = viewModel::updateOwnerEmail,
                                                placeholderText = "Owner Email",
                                                keyboardActions = KeyboardActions(
                                                    onNext = {
                                                        viewModel.ownerAction(OwnerAction.EmailSubmitted)
                                                    }),
                                                error = state.emailContactStateData.validationError,
                                                isLoading = state.isLoading,
                                                keyboardOptions = KeyboardOptions(
                                                    imeAction = ImeAction.Next
                                                )
                                            )
                                        }

                                        ContactState.UNVERIFIED, ContactState.VERIFIED -> {
                                            OwnerInformationRow(
                                                value = "Owner Email: ${state.emailContactStateData.value}",
                                                valueVerified = state.emailContactStateData.verified,
                                                onVerifyClicked = viewModel::verifyOwnerEmail,
                                                onEditClicked = {})
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (state.phoneContactState()) {
                                        ContactState.DOES_NOT_EXIST -> {
                                            OwnerInformationField(
                                                value = state.phoneContactStateData.value,
                                                onValueChange = viewModel::updateOwnerPhone,
                                                placeholderText = "Owner Phone",
                                                keyboardActions = KeyboardActions(onDone = {
                                                    viewModel.ownerAction(OwnerAction.PhoneSubmitted)
                                                }),
                                                error = state.phoneContactStateData.validationError,
                                                isLoading = state.isLoading,
                                                keyboardOptions = KeyboardOptions(
                                                    imeAction = ImeAction.Done
                                                )
                                            )
                                        }

                                        ContactState.UNVERIFIED, ContactState.VERIFIED -> {
                                            OwnerInformationRow(value = "Owner Phone: ${state.phoneContactStateData.value}",
                                                valueVerified = state.phoneContactStateData.verified,
                                                onVerifyClicked = viewModel::verifyOwnerPhone,
                                                onEditClicked = {})
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(34.dp))
                                    //Button to verify information
                                    ElevatedButton(modifier = Modifier.align(Alignment.CenterHorizontally),
                                        onClick = {
                                            Toast.makeText(
                                                context,
                                                "Sending verification codes to submitted email and phone number",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            viewModel.sendVerificationCodesToOwner()
                                        }) {
                                        Text(text = "Resend verify codes")
                                    }
                                }
                            }

                            OwnerInputState.VERIFY_OWNER_EMAIL -> {
                                VerifyCode(
                                    value = state.emailContactStateData.verificationCode,
                                    onValueChange = viewModel::updateEmailVerificationCode,
                                    onDone = {
                                        viewModel.ownerAction(OwnerAction.EmailVerification)
                                    },
                                    isLoading = state.isLoading
                                )
                            }

                            OwnerInputState.VERIFY_OWNER_PHONE -> {
                                VerifyCode(
                                    value = state.phoneContactStateData.verificationCode,
                                    onValueChange = viewModel::updatePhoneVerificationCode,
                                    onDone = {
                                        viewModel.ownerAction(OwnerAction.PhoneVerification)
                                    },
                                    isLoading = state.isLoading
                                )
                            }
                        }
                    }

                    OwnerState.VERIFIED -> {
                        OwnerVerifiedUI(
                            onSubmitClicked = { name: String, email: String ->
                                viewModel.submitGuardian(guardianName = name, guardianEmail = email)
                            }, isLoading = state.isLoading
                        )
                    }

                    OwnerState.GUARDIAN_INVITED -> {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Invited Guardian Status",
                                style = TextStyle.Default.copy(color = Color.Black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Column(
                                    modifier = Modifier.weight(0.75f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(text = "Name: ${state.invitedGuardian.name}")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "Email: ${state.invitedGuardian.email}")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "Guardian Status: ${state.invitedGuardian.invitationStatus.name.lowercase()}")
                                }

                                Column(
                                    modifier = Modifier.weight(0.25f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.invitedGuardian.verificationCode,
                                        style = TextStyle.Default.copy(
                                            color = Color.Black,
                                            fontSize = 26.sp,
                                            letterSpacing = 12.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    //TODO: Setup countdown timer style progression for the progress indicator
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        progress = 0.65f,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerVerifiedUI(
    onSubmitClicked: (name: String, email: String) -> Unit, isLoading: Boolean
) {
    val guardianName = remember {
        mutableStateOf("")
    }

    val guardianEmail = remember {
        mutableStateOf("")
    }

    Text(text = "Owner Verified")
    Spacer(modifier = Modifier.height(36.dp))

    Text(text = "Fill out fields to invite first guardian")
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(value = guardianName.value,
        onValueChange = { guardianName.value = it },
        placeholder = {
            Text(text = "Guardian Name:")
        })

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(value = guardianEmail.value,
        onValueChange = { guardianEmail.value = it },
        placeholder = {
            Text(text = "Guardian Email:")
        })

    Spacer(modifier = Modifier.height(36.dp))
    ElevatedButton(onClick = {
        onSubmitClicked(guardianName.value, guardianEmail.value)
    }) {
        if (isLoading) {
            CircularProgressIndicator(strokeWidth = 2.5.dp)
        } else {
            Text(text = "Send Guardian Invite")
        }

    }
}