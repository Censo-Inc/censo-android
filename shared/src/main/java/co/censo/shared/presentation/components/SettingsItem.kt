package co.censo.shared.presentation.components

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.InvertedButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SettingsItem(
    title: String,
    buttonText: String,
    description: String?,
    onSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(all = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                modifier = Modifier.weight(1.0f)
            )
            Spacer(Modifier.weight(0.025f))
            StandardButton(
                modifier = Modifier.weight(0.5f).fillMaxWidth(),
                color = SharedColors.ButtonTextBlue,
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp),
                onClick = {
                    onSelected()
                },
            ) {
                Text(
                    text = buttonText,
                    style = InvertedButtonTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        description?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = SharedColors.MainColorText,
                fontSize = 14.sp
            )
        }
    }
}

