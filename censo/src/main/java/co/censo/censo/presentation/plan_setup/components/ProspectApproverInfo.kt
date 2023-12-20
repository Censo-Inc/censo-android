package co.censo.censo.presentation.plan_setup.components

import Base58EncodedApproverPublicKey
import Base64EncodedData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.projectLog
import kotlinx.datetime.Clock

@Composable
fun ProspectApproverInfoBox(
    nickName: String,
    status: ApproverStatus?,
    onEdit: (() -> Unit)? = null
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(12.dp), color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = SharedColors.MainBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val labelTextSize = 14.sp

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.approver),
                color = SharedColors.MainColorText,
                fontSize = labelTextSize
            )

            Text(
                text = nickName,
                color = SharedColors.MainColorText,
                fontSize = 24.sp
            )

            activatedUIData(status, context)?.let {
                Text(
                    text = it.text,
                    color = it.color,
                    fontSize = labelTextSize
                )
            }
        }

        onEdit?.let {
            IconButton(onClick = onEdit) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.edit_icon),
                    contentDescription = stringResource(R.string.edit_approver_name),
                    tint = SharedColors.ApproverStepIconColor
                )
            }
        }
    }
}

@Preview
@Composable
fun ProspectApproverInfoBoxLongNamePreview() {
    Box(
        Modifier
            .background(Color.White)
            .padding(24.dp)
    ) {
        ProspectApproverInfoBox(nickName = "Super Long Name To Check The Edit Icon Does Not Shrink",
            status = ApproverStatus.Confirmed(
                approverKeySignature = Base64EncodedData(base64Encoded = "AA"),
                approverPublicKey = Base58EncodedApproverPublicKey(value = "AA"),
                timeMillis = 0,
                confirmedAt = Clock.System.now()
            ),
            onEdit = {
                projectLog(message = "Tapped")
            })
    }
}

@Preview
@Composable
fun ProspectApproverInfoBoxRegularPreview() {
    Box(
        Modifier
            .background(Color.White)
            .padding(24.dp)
    ) {
        ProspectApproverInfoBox(nickName = "Goji", status = ApproverStatus.Confirmed(
            approverKeySignature = Base64EncodedData(base64Encoded = "AA"),
            approverPublicKey = Base58EncodedApproverPublicKey(value = "AA"),
            timeMillis = 0,
            confirmedAt = Clock.System.now()
        ), onEdit = {
            projectLog(message = "Tapped")
        })
    }
}