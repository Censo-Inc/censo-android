package co.censo.vault.presentation.components.security_plan

import Base64EncodedData
import FullScreenButton
import ParticipantId
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Guardian
import co.censo.shared.presentation.Colors
import co.censo.vault.R

enum class SetupSecurityPlanScreen {
    Initial, AddApprovers, RequiredApprovals, Review, SecureYourPlan, FacetecAuth
}

//region Security Plan Top Level Container
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPlanTopLevelContainer(
    moveForward: () -> Unit,
    setupSecurityPlanScreen: SetupSecurityPlanScreen,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current

    val showTwoButtons = when (setupSecurityPlanScreen) {
        SetupSecurityPlanScreen.Initial,
        SetupSecurityPlanScreen.AddApprovers,
        SetupSecurityPlanScreen.RequiredApprovals,
        SetupSecurityPlanScreen.SecureYourPlan -> true
        SetupSecurityPlanScreen.FacetecAuth,
        SetupSecurityPlanScreen.Review -> false
    }

    val bottomButtonText = when (setupSecurityPlanScreen) {
        SetupSecurityPlanScreen.Initial -> stringResource(R.string.select_first_approver_title)
        SetupSecurityPlanScreen.AddApprovers -> stringResource(R.string.next_required_approvals)
        SetupSecurityPlanScreen.RequiredApprovals -> stringResource(R.string.next_review)
        SetupSecurityPlanScreen.Review -> stringResource(R.string.confirm)
        SetupSecurityPlanScreen.SecureYourPlan -> stringResource(id = R.string.continue_text)
        SetupSecurityPlanScreen.FacetecAuth -> ""
    }

    Scaffold(
        contentColor = Color.White,
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Colors.PrimaryBlue),
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIos,
                            stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.setup_security_plan),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (showTwoButtons) {
                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Color.White,
                        textColor = Colors.PrimaryBlue,
                        border = true,
                        contentPadding = PaddingValues(vertical = 6.dp),
                        onClick = {
                            Toast.makeText(
                                context,
                                "Show user explanation box",
                                Toast.LENGTH_LONG
                            ).show()
                        }) {
                        Text(
                            text = stringResource(R.string.how_does_this_work),
                            color = Colors.PrimaryBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W300
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                FullScreenButton(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Colors.PrimaryBlue,
                    textColor = Color.White,
                    border = false,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = moveForward,
                )
                {
                    Text(
                        text = bottomButtonText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
//endregion

//region Security Plan Content Screens
@Composable
fun InitialAddApproverScreen(paddingValues: PaddingValues) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = "Images down here in this container...", color = Color.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProtectionPlanTitle(text = stringResource(R.string.select_approvers))

            val textStyle = SpanStyle(
                fontSize = 18.sp, color = Colors.GreyText
            )

            val annotatedString = buildAnnotatedString {
                withStyle(textStyle) {
                    append(stringResource(R.string.select_approvers_initial_info_explainer))
                }
            }

            ProtectionPlanExplainerBox(
                annotatedString = annotatedString,
                contentPaddingValues = PaddingValues(vertical = 32.dp, horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun RequiredApprovalsScreen(
    paddingValues: PaddingValues,
    guardians: List<Guardian>,
    sliderPosition: Float,
    updateThreshold: (Float) -> Unit
) {
    val oneOrFewerGuardians = guardians.size <= 1

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .background(color = Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProtectionPlanTitle(text = stringResource(R.string.required_approvals))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            val normalStyle = SpanStyle()

            val annotatedString = if (oneOrFewerGuardians) {
                buildAnnotatedString {
                    withStyle(normalStyle) {
                        append(stringResource(R.string.you_have_a_single_approver_so_their_approval_will_be_required_to_access_your_seed_phrases))
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(normalStyle) {
                        append(stringResource(R.string.choose_how_many_approvals_will_be_required_for_you_to_access_your_seed_phrases_we_recommend))
                    }
                    withStyle(
                        normalStyle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W700
                        )
                    ) {
                        append(" ${guardians.size / 2 + 1} ")
                    }
                    withStyle(normalStyle) {
                        append(stringResource(R.string.but_you_can_change_it_below))
                    }
                }
            }

            ProtectionPlanExplainerBox(
                modifier = Modifier.align(Alignment.Center),
                annotatedString = annotatedString,
                paddingValues = PaddingValues(horizontal = 20.dp),
                contentPaddingValues = PaddingValues(vertical = 32.dp, horizontal = 28.dp)
            )
        }

        if (oneOrFewerGuardians) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val rowHorizontalPadding = 8.dp

                Spacer(modifier = Modifier.weight(1.0f))

                ThresholdSlider(
                    sliderPosition = sliderPosition,
                    iconAndLabelHorizontalPadding = rowHorizontalPadding,
                    guardians = guardians,
                    onValueChange = updateThreshold
                )

                Spacer(modifier = Modifier.weight(2.0f))

                ThresholdText(
                    threshold = sliderPosition.toInt(),
                    total = guardians.size,
                    boxStyle = false
                )

                Spacer(modifier = Modifier.weight(1.0f))

            }
        }
    }
}

@Composable
fun SelectApproversScreen(
    paddingValues: PaddingValues,
    guardians: List<Guardian.SetupGuardian>,
    addApproverOnClick: () -> Unit,
    editApproverOnClick: (Guardian.SetupGuardian) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .background(color = Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            ProtectionPlanTitle(text = stringResource(id = R.string.select_approvers))

            AmountGuardiansProtectingText(amountGuardians = guardians.size)

            Spacer(modifier = Modifier.height(12.dp))

            for (guardian in guardians) {
                ApproverRow(
                    approver = guardian,
                ) {
                    editApproverOnClick(guardian)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AddAnotherButton {
                addApproverOnClick()
            }
        }
    }
}

@Composable
fun ReviewPlanScreen(
    sliderPosition: Float,
    paddingValues: PaddingValues,
    guardians: List<Guardian.SetupGuardian>,
    updateThreshold: (Float) -> Unit,
    editApprover: (Guardian.SetupGuardian) -> Unit,
    addApprover: () -> Unit
) {
    val oneOrFewerGuardians = guardians.size <= 1

    Column(
        modifier = Modifier.padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .weight(0.75f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProtectionPlanTitle(
                text = stringResource(R.string.review),
                bottomPadding = 16.dp
            )

            for (guardian in guardians) {
                ApproverRow(approver = guardian) {
                    editApprover(guardian)
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (oneOrFewerGuardians) Arrangement.Top else Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            AddAnotherButton(onClick = addApprover)

            if (oneOrFewerGuardians) {

                Spacer(modifier = Modifier.height(16.dp))

                val annotatedString = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = 18.sp,
                            color = Color.Black,
                        )
                    ) {
                        append(
                            if (guardians.isEmpty()) {
                                stringResource(R.string.you_must_add_at_least_one_approver)
                            } else {
                                stringResource(id = R.string.you_have_a_single_approver_so_their_approval_will_be_required_to_access_your_seed_phrases)
                            }
                        )
                    }
                }

                ProtectionPlanExplainerBox(
                    annotatedString = annotatedString,
                    paddingValues = PaddingValues(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                )

            } else {

                ThresholdText(
                    threshold = sliderPosition.toInt(),
                    total = guardians.size,
                    boxStyle = true,
                    paddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    contentPaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            if (!oneOrFewerGuardians) {

                Spacer(modifier = Modifier.height(12.dp))

                ThresholdSlider(
                    sliderPosition = sliderPosition,
                    iconAndLabelHorizontalPadding = 8.dp,
                    totalPadding = PaddingValues(horizontal = 36.dp),
                    guardians = guardians,
                    onValueChange = updateThreshold
                )
            }
        }
    }

}

@Composable
fun SecureYourPlanScreen(paddingValues: PaddingValues) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = "", color = Color.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProtectionPlanTitle(text = stringResource(R.string.establish_your_identity))

            val textStyle = SpanStyle(
                fontSize = 18.sp, color = Colors.GreyText
            )

            val annotatedString = buildAnnotatedString {
                withStyle(textStyle) {
                    append(stringResource(R.string.establish_identity_explainer))
                }
            }

            ProtectionPlanExplainerBox(
                annotatedString = annotatedString,
                contentPaddingValues = PaddingValues(vertical = 32.dp, horizontal = 20.dp)
            )
        }
    }
}

//endregion

//region test + previews
@Composable
fun TestableProtectionScreen(initialPosition: Int) {

    val guardians = listOf(
        Guardian.SetupGuardian(
            "Ben",
            ParticipantId(generatePartitionId().toHexString()),
            deviceEncryptedTotpSecret = Base64EncodedData("")
        ),
        Guardian.SetupGuardian(
            "A.L.",
            ParticipantId(generatePartitionId().toHexString()),
            deviceEncryptedTotpSecret = Base64EncodedData("")
        ),
        Guardian.SetupGuardian(
            "Carlitos",
            ParticipantId(generatePartitionId().toHexString()),
            deviceEncryptedTotpSecret = Base64EncodedData("")
        ),
    )

    var screenPosition by remember { mutableStateOf(initialPosition) }
    var sliderPosition by remember { mutableStateOf(1.0f) }

    val selectedScreen: SetupSecurityPlanScreen =
        if (screenPosition >= 4) {
            SetupSecurityPlanScreen.SecureYourPlan
        } else
        if (screenPosition >= 3) {
            SetupSecurityPlanScreen.Review
        } else if (screenPosition == 2) {
            SetupSecurityPlanScreen.RequiredApprovals
        } else if (screenPosition == 1) {
            SetupSecurityPlanScreen.AddApprovers
        } else {
            SetupSecurityPlanScreen.Initial
        }


    SecurityPlanTopLevelContainer(
        setupSecurityPlanScreen = selectedScreen,
        moveForward = {
            screenPosition = if (screenPosition >= 4) {
                0
            } else if (screenPosition == 3) {
                4
            } else if (screenPosition == 2) {
                3
            } else if (screenPosition == 1) {
                2
            } else {
                1
            }
        }
    ) {
        when (screenPosition) {
            1 -> SelectApproversScreen(
                paddingValues = it,
                guardians = guardians,
                addApproverOnClick = { },
                editApproverOnClick = { }
            )

            2 -> RequiredApprovalsScreen(
                paddingValues = it,
                guardians = guardians,
                sliderPosition = sliderPosition
            ) { updatedSlider ->
                sliderPosition = updatedSlider
            }

            3 -> ReviewPlanScreen(
                paddingValues = it,
                guardians = guardians,
                sliderPosition = sliderPosition,
                updateThreshold = { updatedPosition ->
                    sliderPosition = updatedPosition
                },
                editApprover = {},
                addApprover = {}
            )
            4 -> SecureYourPlanScreen(
                paddingValues = it
            )
            else -> InitialAddApproverScreen(paddingValues = it)
        }

    }
}

@Preview
@Composable
fun Preview() {
    TestableProtectionScreen(0)
}
//endregion