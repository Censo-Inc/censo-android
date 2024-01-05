package co.censo.approver.presentation.reset_links.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ListOwnersUI(
    approverStates: List<ApproverState>,
    onParticipantIdSelected: (ParticipantId) -> Unit,
    selectedParticipantId: ParticipantId?,
    onContinue: () -> Unit,
) {

    val buttonEnabled = selectedParticipantId != null

    Box(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 36.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            MessageText(
                message = stringResource(R.string.please_select_the_person_that_has_contacted_you),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left
            )

            Spacer(modifier = Modifier.height(30.dp))

            approverStates.forEach { approverState ->
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
                        .padding(
                            horizontal = 20.dp,
                            vertical = 20.dp
                        )
                        .clickable {
                            onParticipantIdSelected(approverState.participantId)
                        },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(25.dp)
                            .align(Alignment.CenterVertically),
                    ) {
                        if (approverState.participantId == selectedParticipantId) {
                            Icon(
                                painterResource(id = co.censo.shared.R.drawable.check_icon),
                                contentDescription = stringResource(R.string.select_person),
                                tint = SharedColors.MainIconColor
                            )
                        }
                    }

                    Text(
                        text = approverState.ownerLabel ?: "-",
                        color = SharedColors.MainColorText,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter),
            onClick = onContinue,
            enabled = buttonEnabled,
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.continue_text),
                style = if (buttonEnabled) ButtonTextStyle.copy(fontSize = 22.sp) else DisabledButtonTextStyle.copy(fontSize = 22.sp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ListApproversUIPreview() {
    Box(
        modifier = Modifier.background(Color.White)
    ) {
        val participantId = ParticipantId.generate()
        ListOwnersUI(
            approverStates = listOf(
                ApproverState(
                    participantId = ParticipantId.generate(),
                    phase = ApproverPhase.Complete,
                ),
                ApproverState(
                    participantId = participantId,
                    phase = ApproverPhase.Complete,
                    ownerLabel = "some label"
                )
            ),
            selectedParticipantId = participantId,
            onParticipantIdSelected = {},
            onContinue = {}
        )
    }
}