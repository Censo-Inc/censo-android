package co.censo.censo.presentation.plan_setup.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.R

@Composable
fun ApproversRemoved() {
    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painterResource(id = co.censo.shared.R.drawable.check_circle),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.approvers_removed),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ApproversRemovedPreview() {
    ApproversRemoved()
}