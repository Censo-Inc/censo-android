package co.censo.censo.presentation.plan_setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.SharedColors

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
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
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

        Column {
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

        //TODO: Fix this resizing itself
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

//TODO: Setup preview for quick iteration