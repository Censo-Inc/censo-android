package co.censo.censo.presentation.access_approval.components

import LearnMore
import MessageText
import ParticipantId
import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import co.censo.censo.R
import kotlinx.datetime.Clock

@Composable
fun SelectApproverUI(
    approvers: List<Guardian.TrustedGuardian>,
    selectedApprover: Guardian.TrustedGuardian?,
    onApproverSelected: (Guardian.TrustedGuardian) -> Unit,
    onContinue: () -> Unit
) {

    val buttonEnabled = selectedApprover != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            title = R.string.request_access
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            message = "Which approver would you like to use to request access?"
        )

        Spacer(modifier = Modifier.height(24.dp))

        approvers.sortedBy { it.attributes.onboardedAt }.forEachIndexed { index, approver ->
            Spacer(modifier = Modifier.height(12.dp))

            ApproverInfoBox(
                nickName = approver.label,
                primaryApprover = (index == 0),
                selected = (approver == selectedApprover),
                onSelect = { onApproverSelected(approver) }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonEnabled,
            disabledColor = SharedColors.DisabledGrey,
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onContinue
        ) {
            Text(
                fontSize = 20.sp,
                text = stringResource(id = R.string.continue_text),
                color = if (buttonEnabled) Color.White else SharedColors.DisabledFontGrey
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(24.dp))

    }
}

@Composable
fun ApproverInfoBox(
    nickName: String,
    primaryApprover: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
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
                color = if (selected) Color.Black else SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onSelect() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        val labelTextSize = 14.sp

        Box(
            modifier = Modifier
                .width(25.dp)
                .align(Alignment.CenterVertically),
        ) {
            if (selected) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.check_icon),
                    contentDescription = stringResource(R.string.select_approver),
                    tint = Color.Black
                )
            }
        }

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

            Text(
                text = context.getString(R.string.active),
                color = SharedColors.SuccessGreen,
                fontSize = labelTextSize
            )

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectApproverUIPreview() {
    val primaryApprover = Guardian.TrustedGuardian(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Guardian.TrustedGuardian(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApproverUI(
        approvers = listOf(
            primaryApprover,
            backupApprover
        ),
        selectedApprover = backupApprover,
        onApproverSelected = {},
        onContinue = {}
    )
}