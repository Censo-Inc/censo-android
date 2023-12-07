package co.censo.censo.presentation.main

import MessageText
import StandardButton
import TitleText
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.censo.presentation.plan_setup.components.ApproverActivatedUIData
import kotlinx.datetime.Clock

@Composable
fun ApproversHomeScreen(
    approvers: List<Guardian.TrustedGuardian>,
    approverSetupExists: Boolean,
    onInviteApproversSelected: () -> Unit,
    onRemoveApproversSelected: () -> Unit
) {
    val verticalSpacingHeight = 24.dp

    val nonOwnerApprovers = approvers.filter { !it.isOwner }

    if (nonOwnerApprovers.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            nonOwnerApprovers.sortedBy { it.attributes.onboardedAt }.forEach { approver ->
                ApproverInfoBox(
                    nickName = approver.label,
                    status = approver.attributes,
                    editEnabled = false
                )
                Spacer(modifier = Modifier.height(verticalSpacingHeight))
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                onClick = onRemoveApproversSelected,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.approvers),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = pluralStringResource(id = R.plurals.remove_approvers, count = nonOwnerApprovers.size),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W400
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pluralStringResource(R.plurals.remove_approvers_span, count = nonOwnerApprovers.size),
                textAlign = TextAlign.Start,
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))
        }
    } else {
        NoApproversUI(
            approverSetupExists = approverSetupExists,
            onInviteApproversSelected = onInviteApproversSelected
        )
    }
}

@Composable
fun NoApproversUI(
    approverSetupExists: Boolean,
    onInviteApproversSelected: () -> Unit
) {
    val verticalSpacingHeight = 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.you_can_increase_your_security),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 12.dp))

        MessageText(
            message = buildSpannedParagraph(
                preceding = stringResource(R.string.adding_approvers_makes_you_more_secure_span),
                bolded = stringResource(R.string.require),
                remaining = stringResource(R.string.an_approval_from_one_of_your_approvers),
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            message = stringResource(R.string.adding_approvers_ensures_span),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onInviteApproversSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.approvers),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (approverSetupExists) {
                    Text(
                        text = stringResource(R.string.resume_adding_approvers_button_text),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W400,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = stringResource(R.string.add_approvers_button_text),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))
    }
}

private fun buildSpannedParagraph(
    preceding: String,
    bolded: String,
    remaining: String
): AnnotatedString {
    val boldSpanStyle = SpanStyle(
        fontWeight = FontWeight.W700
    )

    return buildAnnotatedString {
        append("$preceding ")
        withStyle(boldSpanStyle) {
            append(bolded)
        }
        append(" $remaining")
    }
}

@Composable
fun ApproverInfoBox(
    nickName: String,
    status: GuardianStatus?,
    editEnabled: Boolean = true
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val labelTextSize = 15.sp

        Column {
            Text(
                text = stringResource(id = R.string.approver),
                color = Color.Black,
                fontSize = labelTextSize
            )

            Text(
                text = nickName,
                color = Color.Black,
                fontSize = 24.sp
            )

            val activatedUIData: ApproverActivatedUIData = activatedUIData(status, context)

            Text(
                text = activatedUIData.text,
                color = activatedUIData.color,
                fontSize = labelTextSize
            )
        }

        if (editEnabled) {

            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.edit_icon),
                    contentDescription = stringResource(R.string.edit_approver_name),
                    tint = Color.Black
                )
            }
        }
    }
}

fun activatedUIData(guardianStatus: GuardianStatus?, context: Context) =
    when (guardianStatus) {
        is GuardianStatus.Initial,
        is GuardianStatus.Accepted,
        GuardianStatus.Declined -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.ErrorRed
            )
        }

        else -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.active),
                color = SharedColors.SuccessGreen
            )
        }
    }


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewEmptyApproversHome() {
    ApproversHomeScreen(
        approvers = emptyList(),
        approverSetupExists = false,
        onInviteApproversSelected = {},
        onRemoveApproversSelected = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewEmptyApproversResumeHome() {
    ApproversHomeScreen(
        approvers = emptyList(),
        approverSetupExists = true,
        onInviteApproversSelected = {},
        onRemoveApproversSelected = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewSingleApproverHome() {
    ApproversHomeScreen(
        approvers = listOf(
            Guardian.TrustedGuardian(
                label = "Neo",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = GuardianStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
        ),
        approverSetupExists = false,
        onInviteApproversSelected = {},
        onRemoveApproversSelected = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewMultipleApproversHome() {
    ApproversHomeScreen(
        approvers = listOf(
            Guardian.TrustedGuardian(
                label = "Neo",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = GuardianStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
            Guardian.TrustedGuardian(
                label = "John Wick",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = GuardianStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            )
        ),
        approverSetupExists = false,
        onInviteApproversSelected = {},
        onRemoveApproversSelected = {},
    )
}