package co.censo.censo.presentation.plan_setup.components

import Base58EncodedApproverPublicKey
import Base64EncodedData
import InvitationId
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock

@Composable
fun Activated(
    firstApprover: Approver.ProspectApprover?,
    secondApprover: Approver.ProspectApprover?,
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.confetti),
            contentDescription = null,
            contentScale = ContentScale.None,
        )

        Column(
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.approvers_activated),
                color = SharedColors.MainColorText,
                fontSize = 32.sp
            )

            firstApprover?.let {
                Spacer(modifier = Modifier.weight(0.1f))
                ProspectApproverInfoBox(
                    nickName = firstApprover.label,
                    status = firstApprover.status
                )
            }

            secondApprover?.let {
                Spacer(modifier = Modifier.weight(0.1f))
                ProspectApproverInfoBox(
                    nickName = secondApprover.label,
                    status = secondApprover.status
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivatedUITwoApproversPreview() {
    val status = ApproverStatus.Confirmed(
        approverPublicKey = Base58EncodedApproverPublicKey(""),
        approverKeySignature = Base64EncodedData(""),
        timeMillis = 0,
        confirmedAt = Clock.System.now()
    )

    Activated(
        firstApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = status
        ),
        secondApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = status
        ),
    )
}