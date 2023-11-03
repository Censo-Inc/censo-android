package co.censo.censo.presentation.components.owner_information

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MobileFriendly
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun OwnerInformationRow(
    value: String,
    valueVerified: Boolean,
    onVerifyClicked: () -> Unit,
    onEditClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = value
        )

        IconButton(
            modifier = Modifier.weight(0.25f),
            onClick = onVerifyClicked
        ) {
            Icon(
                imageVector = Icons.Default.MobileFriendly,
                contentDescription = "Verify",
                tint = if (valueVerified) Color.Green else LocalContentColor.current
            )
        }

        IconButton(
            modifier = Modifier.weight(0.25f),
            onClick = onEditClicked
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit"
            )
        }
    }
}