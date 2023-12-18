package co.censo.approver.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.shared.presentation.SharedColors
import co.censo.shared.R as SharedR

@Composable
fun PostApproverAction() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        val sharedFontSize = 22.sp

        Image(
            painterResource(id = SharedR.drawable.check_circle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor),
        )


        Text(
            text = stringResource(R.string.congratulations_you_re_all_done),
            fontSize = sharedFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
            lineHeight =  30.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.thanks_for_helping_approver),
            fontSize = sharedFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.you_may_now_close_the_app),
            fontSize = sharedFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
            lineHeight =  30.sp
        )
    }
}

@Preview
@Composable
fun PostApproverActionPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        PostApproverAction()
    }
}