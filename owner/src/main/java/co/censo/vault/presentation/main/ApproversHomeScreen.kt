package co.censo.vault.presentation.main

import LearnMore
import MessageText
import StandardButton
import SubTitleText
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.SharedColors
import co.censo.vault.R
import co.censo.vault.presentation.plan_setup.components.ApproverActivatedUIData

@Composable
fun ApproversHomeScreen(
    guardians: List<Guardian.TrustedGuardian>,
    onInviteApproversSelected: () -> Unit
) {
    if (guardians.any { it.label != "Me" }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            for (guardian in guardians) {
                ApproverInfoBox(
                    nickName = guardian.label,
                    primaryApprover = true,
                    status = guardian.attributes,
                    editEnabled = false
                )
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
    val verticalSpacingHeight = 12.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        SubTitleText(
            modifier = Modifier.fillMaxWidth(),
            subtitle = R.string.optional_increase_security,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.invite_trusted_approvers,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))


        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.invite_trusted_approvers_message,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))

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
                    text = stringResource(R.string.invite_approver),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
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
                text =
                if (primaryApprover) stringResource(R.string.primary_approver)
                else stringResource(R.string.alternate_approver),
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
                text = context.getString(R.string.activated),
                color = SharedColors.SuccessGreen
            )
        }
    }


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewApproversHome() {
    ApproversHomeScreen(
        guardians = emptyList(),
        onInviteApproversSelected = {}
//        listOf(
//            Guardian.TrustedGuardian(
//                label = "Neo",
//                participantId = ParticipantId.generate(),
//                attributes = GuardianStatus.Onboarded(
//                    onboardedAt = Clock.System.now()
//                )
//            )
//        )
    )
}