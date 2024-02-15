package co.censo.censo.presentation.legacy_information.components

import ParticipantId
import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.DecryptedApproverContactInfo
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ApproversContactInfoUI(
    contactInfo: List<DecryptedApproverContactInfo>,
    onSave: (updatedContactInfo: List<DecryptedApproverContactInfo>) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.025f

    val contactInfoStates = contactInfo.map { mutableStateOf(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        contactInfoStates.forEach {
            Spacer(modifier = Modifier.height(verticalSpacingHeight))
            ApproverContactInformationUI(
                verticalSpacingHeight = verticalSpacingHeight,
                contactInfoState = it
            )
            Spacer(modifier = Modifier.height(verticalSpacingHeight))
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave(contactInfoStates.map { it.value }) },
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.save),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.W400)
            )
        }
        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        Spacer(modifier = Modifier.weight(0.7f))
    }
}

@Composable
fun ApproverContactInformationUI(
    verticalSpacingHeight: Dp,
    contactInfoState: MutableState<DecryptedApproverContactInfo>
) {
    Column {
        TitleText(
            title = contactInfoState.value.label,
        )
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    shape = RoundedCornerShape(12.dp)
                ),
            value = contactInfoState.value.contactInfo,
            singleLine = false,
            minLines = 5,
            maxLines = 5,
            onValueChange = { newValue ->
                contactInfoState.value = contactInfoState.value.copy(contactInfo = newValue)
            },
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = SharedColors.MainColorText
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 14.dp)
                ) {

                    if (contactInfoState.value.contactInfo.isEmpty()) {
                        Text(
                            text = stringResource(R.string.enter_contact_information),
                            style = TextStyle(
                                fontSize = 20.sp,
                                color = Color.Gray
                            )
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeApproverInfoUI() {
    ApproversContactInfoUI(
        contactInfo = listOf(
            DecryptedApproverContactInfo(
                participantId = ParticipantId("00"),
                label = "Neo",
                contactInfo = "telegram @neo"
            ),
            DecryptedApproverContactInfo(
                participantId = ParticipantId("01"),
                label = "John Wick",
                contactInfo = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed vitae nisi finibus, fermentum nisl in, aliquet ante."
            )
        ),
        onSave = {}
    )
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumApproverInfoUI() {
    ApproversContactInfoUI(
        contactInfo = listOf(
            DecryptedApproverContactInfo(
                participantId = ParticipantId("00"),
                label = "Neo",
                contactInfo = "telegram @neo"
            ),
            DecryptedApproverContactInfo(
                participantId = ParticipantId("01"),
                label = "John Wick",
                contactInfo = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed vitae nisi finibus, fermentum nisl in, aliquet ante."
            )
        ),
        onSave = {}
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallApproverInfoUI() {
    ApproversContactInfoUI(
        contactInfo = listOf(
            DecryptedApproverContactInfo(
                participantId = ParticipantId("00"),
                label = "Neo",
                contactInfo = "telegram @neo"
            ),
            DecryptedApproverContactInfo(
                participantId = ParticipantId("01"),
                label = "John Wick",
                contactInfo = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed vitae nisi finibus, fermentum nisl in, aliquet ante."
            )
        ),
        onSave = {}
    )
}