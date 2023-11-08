package co.censo.approver.presentation.entrance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.approver.presentation.components.PasteLink

@Composable
fun LoggedInPasteLinkUI(
    isApprover: Boolean,
    onPasteLinkClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            PasteLink(
                onPasteLinkClick = onPasteLinkClick
            )

            Spacer(modifier = Modifier.weight(0.7f))
        }

        if (isApprover) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painterResource(
                        id = R.drawable.active_approvers_icon
                    ),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active approver",
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.W500
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoggedInPasteLinkUIPreview() {
    LoggedInPasteLinkUI(
        isApprover = true,
        onPasteLinkClick = {}
    )
}
