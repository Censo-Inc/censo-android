package co.censo.shared.presentation.components

import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun DisplayError(
    modifier: Modifier = Modifier,
    errorMessage: String,
    dismissAction: (() -> Unit)?,
    retryAction: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
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
            }
            .padding(all = 24.dp),
        contentAlignment = Alignment.Center,
    ) {

        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    shape = RoundedCornerShape(12.dp)
                )
                .zIndex(2.5f)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {

            dismissAction?.let {
                IconButton(onClick = it) {
                    Icon(
                        modifier = Modifier.size(44.dp),
                        imageVector = Icons.Default.Close,
                        contentDescription = "dismiss error alert"
                    )
                }
            } ?: Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(12.dp))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = "Something went wrong"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = errorMessage,
                color = Color.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )


            retryAction?.let {
                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    StandardButton(
                        onClick = retryAction,
                        contentPadding = PaddingValues(horizontal = 44.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.retry),
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewDisplayError() {
    DisplayError(
        errorMessage = "This is our error message",
        dismissAction = {},
        retryAction = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewNoDismissDisplayError() {
    DisplayError(
        errorMessage = "This is our error message",
        dismissAction = {},
        retryAction = null
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewNoRetryDisplayError() {
    DisplayError(
        errorMessage = "This is our error message",
        dismissAction = null,
        retryAction = {}
    )
}