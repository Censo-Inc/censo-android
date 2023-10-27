package co.censo.shared.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.censo.shared.R

@Composable
fun DisplayError(
    modifier: Modifier = Modifier,
    errorMessage: String,
    dismissAction: (() -> Unit)?,
    retryAction: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.White)
            .let {
                if (dismissAction != null) {
                    it.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { dismissAction() }
                } else it
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = errorMessage,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (retryAction != null) {
            TextButton(onClick = retryAction) {
                Text(text = stringResource(R.string.retry))
            }
        }
        if (dismissAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = dismissAction) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    }
}