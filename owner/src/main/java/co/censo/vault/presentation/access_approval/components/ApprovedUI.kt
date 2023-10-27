package co.censo.vault.presentation.access_approval.components

import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.vault.R

@Composable
fun ApprovedUI() {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(id = R.drawable.check_circle),
            contentDescription = "check"
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.approved,
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 100.dp))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApprovedUIPreview() {
    ApprovedUI()
}