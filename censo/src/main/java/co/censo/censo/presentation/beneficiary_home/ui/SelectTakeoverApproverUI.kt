package co.censo.censo.presentation.beneficiary_home.ui

import Base64EncodedData
import ParticipantId
import StandardButton
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.DeepLinkURI
import co.censo.shared.data.model.BeneficiaryApproverContactInfo
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectTakeoverApproverUI(
    approvers: List<BeneficiaryApproverContactInfo>,
    selectedApprover: ParticipantId,
    takeoverId: String,
    onSelectedApprover: (BeneficiaryApproverContactInfo) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = {
        approvers.size
    })

    LaunchedEffect(pagerState) {
        // Collect from the a snapshotFlow reading the currentPage
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelectedApprover(approvers[page])
        }
    }

    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val largeHeight = screenHeight * 0.025f
    val mediumHeight = screenHeight * 0.0125f

    val link = DeepLinkURI.createTakeoverApproverDeepLink(selectedApprover.value, takeoverId)

    fun shareLink(link: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(
                R.string.takeover_initiatied_message
            ),
            color = SharedColors.MainColorText,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.weight(0.10f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 24.dp
        ) { page ->
            val approver = approvers[page]
            ApproverContactInfo(nickName = approver.label)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .background(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        StandardButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = { shareLink(link) }
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    tint = SharedColors.ButtonIconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.share),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
        }
    }
}

@Composable
fun ApproverContactInfo(nickName: String) {

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
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
            .padding(PaddingValues(horizontal = 20.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val labelTextSize = 14.sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = nickName,
                color = SharedColors.MainColorText,
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.contact_information),
                color = SharedColors.MainColorText,
                fontSize = labelTextSize,
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center
            )
            Text(
                text = "***need to add contact details here***",
                color = SharedColors.MainIconColor,
                fontSize = labelTextSize,
                modifier = Modifier.padding(bottom = 2.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSelectTakeoverApprover() {
    val selectedApprover = ParticipantId.generate()
    SelectTakeoverApproverUI(
        approvers = listOf(
            BeneficiaryApproverContactInfo(
                participantId = selectedApprover,
                label = "Sam",
                encryptedContactInfo = Base64EncodedData("")
            ),
            BeneficiaryApproverContactInfo(
                participantId = ParticipantId.generate(),
                label = "Jason",
                encryptedContactInfo = Base64EncodedData("")
            )
        ),
        selectedApprover = selectedApprover,
        takeoverId = "link",
        onSelectedApprover = {}
    )
}