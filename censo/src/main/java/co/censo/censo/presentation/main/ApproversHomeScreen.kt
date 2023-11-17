package co.censo.censo.presentation.main

import MessageText
import StandardButton
import TitleText
import android.content.Context
import android.provider.CalendarContract.Colors
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

@Composable
fun ApproversHomeScreen(
    approvers: List<Guardian.TrustedGuardian>,
    onInviteApproversSelected: () -> Unit
) {
    
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

            for ((index, approver) in nonOwnerApprovers.withIndex()) {
                ApproverInfoBox(
                    nickName = approver.label,
                    primaryApprover = index == 0,
                    status = approver.attributes,
                    editEnabled = false
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

        }
    } else {
        NoApproversUI(
            onInviteApproversSelected = onInviteApproversSelected
        )
    }
}

@Composable
fun NoApproversUI(
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
                preceding = stringResource(R.string.adding_approvers_span),
                bolded = stringResource(R.string.require),
                remaining = stringResource(R.string.their_approval_in_addition_to_yours_span),
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            message = buildSpannedParagraph(
                preceding = stringResource(R.string.adding_a_span),
                bolded = stringResource(R.string.first_approver),
                remaining = stringResource(R.string.ensures_access_first_approver_span),
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            message = buildSpannedParagraph(
                preceding =  stringResource(R.string.adding_a_span),
                bolded = stringResource(R.string.second_approver),
                remaining = stringResource(R.string.ensures_access_second_approver_span),
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight - 12.dp))
        Text(
            text = stringResource(R.string.add_approvers_beta_warning),
            color = Color.Red,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onInviteApproversSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
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
                    text = stringResource(R.string.add_approvers_button_text),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W400
                )
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
    primaryApprover: Boolean,
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
fun PreviewApproversHome() {
    ApproversHomeScreen(
        approvers = emptyList(),
        onInviteApproversSelected = {}
    )
}