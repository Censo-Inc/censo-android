package co.censo.approver.presentation.entrance.components

import StandardButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R

@Composable
fun ApproverLanding(
    isLoggedIn: Boolean = false,
    onActiveApproverLongPress: () -> Unit,
    onContinue: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val linkTag = "link"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = stringResource(R.string.hello_approver), fontSize = 38.sp)

        Spacer(modifier = Modifier.height(48.dp))

        val message =
            if (isLoggedIn) stringResource(R.string.logged_in_approver_landing_message) else stringResource(
                R.string.approver_you_have_been_chosen
            )

        Text(
            text = message,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(48.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
            onClick = onContinue
        ) {
            Text(
                text = stringResource(id = R.string.continue_text),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.W400
            )
        }


        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(48.dp))

            val preceding = stringResource(R.string.interested_in_using_censo_span)
            val remaining = stringResource(R.string.to_download_the_censo_app_span)

            val builtText = buildAnnotatedString {
                append("$preceding ")

                pushStringAnnotation(
                    tag = linkTag,
                    annotation = stringResource(R.string.https_censo_co_link)
                )
                withStyle(style = SpanStyle(fontWeight = FontWeight.W600)) {
                    append(stringResource(R.string.this_link))
                }
                pop()

                append(" $remaining")
            }

            ClickableText(
                text = builtText,
                style = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
            ) { offset ->
                builtText.getStringAnnotations(linkTag, start = offset, end = offset).firstOrNull()
                    ?.let { annotatedLink ->
                        uriHandler.openUri(annotatedLink.item)
                    }
            }
        }
    }

    if (isLoggedIn) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                onActiveApproverLongPress()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painterResource(
                        id = R.drawable.active_approvers_icon
                    ), contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.active_approver),
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.W500
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Preview
@Composable
fun ApproverLandingScreenPreview() {
    ApproverLanding(
        onActiveApproverLongPress = {}
    ) { }
}