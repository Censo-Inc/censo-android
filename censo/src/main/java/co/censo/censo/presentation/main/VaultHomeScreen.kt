package co.censo.censo.presentation.main

import StandardButton
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.plan_setup.components.ApproverActivatedUIData
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock

@Composable
fun VaultHomeScreen(
    seedPhrasesSaved: Int,
    approverSetupExists: Boolean,
    approvers: List<Approver.TrustedApprover>,
    onAddSeedPhrase: () -> Unit,
    onAddApprovers: () -> Unit,
) {
    val context = LocalContext.current

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val bigSpace = screenHeight * 0.08f
    val mediumSpace = screenHeight * 0.04f

    val approverTextTitleStyle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.W600,
        lineHeight = 36.sp,
        textAlign = TextAlign.Start,
        color = SharedColors.MainColorText
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.dashboard),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(bigSpace))

        Column(modifier = Modifier
            .background(shape = RoundedCornerShape(16.dp), color = SharedColors.BottomNavBarIndicatorColor)
            .padding(vertical = 32.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = buildSeedPhraseCount(seedPhrasesSaved, context),
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center,
                color = SharedColors.MainColorText
            )

            Spacer(modifier = Modifier.height(mediumSpace))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = buildAddSeedPhraseDisclaimer(
                    context = context,
                    multiplePhrases = seedPhrasesSaved != 1
                ),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = SharedColors.MainColorText
            )

        }

        val ownerOnly = approvers.size <= 1

        Spacer(modifier = Modifier.height(bigSpace))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (ownerOnly) stringResource(R.string.you_can_increase_security_by_adding_approvers) else stringResource(
                R.string.added_security_with_approvers
            ),
            style = approverTextTitleStyle
        )

        Spacer(modifier = Modifier.height(mediumSpace))

        if (!ownerOnly) {
            val nonOwnerApprovers = approvers.filter { !it.isOwner }

            nonOwnerApprovers.sortedBy { it.attributes.onboardedAt }.forEach { approver ->
                ApproverInfoBox(
                    nickName = approver.label,
                    status = approver.attributes,
                    editEnabled = false
                )
                Spacer(modifier = Modifier.height(mediumSpace))
            }
            Spacer(modifier = Modifier.height(mediumSpace))
        } else {

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = onAddApprovers
            ) {
                Text(
                    text = if(approverSetupExists) stringResource(R.string.resume_adding_approvers) else stringResource(id = R.string.add_approvers_button_text),
                    style = ButtonTextStyle.copy(fontWeight = FontWeight.W400)
                )
            }
        }
    }
}

private fun buildSeedPhraseCount(count: Int, context: Context): AnnotatedString {
    val countSpanStyle = SpanStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.W700,
        color = SharedColors.MainColorText
    )

    val textSpanStyle = SpanStyle(
        fontSize = 24.sp,
        color = SharedColors.MainColorText
    )

    return buildAnnotatedString {
        withStyle(textSpanStyle) {
            append(context.getString(R.string.you_have_span))
        }
        withStyle(countSpanStyle) {
            append(count.toString())
        }
        withStyle(textSpanStyle) {
            if (count != 1) {
                append(context.getString(R.string.seed_phrases_span))
            } else {
                append(context.getString(R.string.seed_phrase_span))
            }
        }
    }
}

private fun buildAddSeedPhraseDisclaimer(
    context: Context,
    multiplePhrases: Boolean
): AnnotatedString {
    val emphasisSpanStyle = SpanStyle(
        fontWeight = FontWeight.W600,
        color = SharedColors.MainColorText
    )

    return buildAnnotatedString {
        if (multiplePhrases) {
            append(context.getString(R.string.they_are_stored_securely))
        } else {
            append(context.getString(R.string.it_is_stored_securely_and_accessible_span))
        }

        append(" ")

        withStyle(emphasisSpanStyle) {
            append(context.getString(R.string.only_span))
        }

        append(context.getString(R.string.to_you_span))
    }
}

@Composable
fun ApproverInfoBox(
    nickName: String,
    status: ApproverStatus?,
    editEnabled: Boolean = true
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val labelTextSize = 15.sp

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.height(42.dp)
                    .padding(horizontal = 8.dp),
                painter = painterResource(id = R.drawable.person_fill),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(SharedColors.MainColorText)
            )

            Column {
                Text(
                    text = nickName,
                    color = SharedColors.MainColorText,
                    fontSize = 24.sp
                )

                val activatedUIData: ApproverActivatedUIData = activatedUIData(status, context)

                Text(
                    text = activatedUIData.text,
                    color = activatedUIData.color,
                    fontSize = labelTextSize,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        if (editEnabled) {

            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.edit_icon),
                    contentDescription = stringResource(R.string.edit_approver_name),
                    tint = Color.Black
                )
            }
        }
    }
}

fun activatedUIData(approverStatus: ApproverStatus?, context: Context) =
    when (approverStatus) {
        is ApproverStatus.Initial,
        is ApproverStatus.Accepted,
        ApproverStatus.Declined -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.ErrorRed
            )
        }

        else -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.active),
                color = SharedColors.SuccessGreen
            )
        }
    }

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun NoApproversVaultHomePreview() {
    VaultHomeScreen(
        onAddApprovers = {},
        onAddSeedPhrase = {},
        approvers = emptyList(),
        approverSetupExists = false,
        seedPhrasesSaved = 2,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ResumeApproversVaultHomePreview() {
    VaultHomeScreen(
        onAddApprovers = {},
        onAddSeedPhrase = {},
        approvers = emptyList(),
        approverSetupExists = true,
        seedPhrasesSaved = 2,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OneApproverVaultHomePreview() {
    VaultHomeScreen(
        onAddApprovers = {},
        onAddSeedPhrase = {},
        approvers = listOf(
            Approver.TrustedApprover(
                label = "Me",
                participantId = ParticipantId.generate(),
                isOwner = true,
                attributes = ApproverStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
            Approver.TrustedApprover(
                label = "Neo",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = ApproverStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
        ),
        approverSetupExists = false,
        seedPhrasesSaved = 2,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun TwoApproversVaultHomePreview() {
    VaultHomeScreen(
        onAddApprovers = {},
        onAddSeedPhrase = {},
        approvers = listOf(
            Approver.TrustedApprover(
                label = "Me",
                participantId = ParticipantId.generate(),
                isOwner = true,
                attributes = ApproverStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
            Approver.TrustedApprover(
                label = "Neo",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = ApproverStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            ),
            Approver.TrustedApprover(
                label = "John Wick",
                participantId = ParticipantId.generate(),
                isOwner = false,
                attributes = ApproverStatus.Onboarded(
                    onboardedAt = Clock.System.now(),
                )
            )
        ),
        approverSetupExists = false,
        seedPhrasesSaved = 2,
    )
}
