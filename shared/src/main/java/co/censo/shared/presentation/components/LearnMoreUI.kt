package co.censo.shared.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun LearnMoreUI(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = SharedColors.MainIconColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.learn_more),
            color = SharedColors.MainColorText,
            fontSize = 18.sp
        )
    }
}

@Composable
fun LearnMoreScreen(
    title: String,
    annotatedString: AnnotatedString,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    Column(
        modifier = Modifier.background(color = Color.White).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            text = title,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontSize = 36.sp,
            fontWeight = FontWeight.W400,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(screenHeight * .010f))
        Divider(
            modifier = Modifier
                .height(5.dp)
                .width(screenWidth * .15f),
            color = SharedColors.BrightDividerColor
        )
        Spacer(modifier = Modifier.height(screenHeight * .010f))
        Text(
            text = annotatedString,
            color = SharedColors.MainColorText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}

object LearnMoreUtil {
    fun trustedApproversMessage(): AnnotatedString {
        val basicStyle = SpanStyle(
            color = SharedColors.MainColorText,
            fontSize = 18.sp
        )

        return buildAnnotatedString {
            withStyle(basicStyle) {
                append(
                    "Censo's approach to securing your cryptographically secured seed phrases and user credentials is innovative yet intuitive. By encrypting the seed phrase on your device and then distributing cryptographically provable approval rights to Trusted Approvers using Shamir Secret Sharing, we ensure maximum security and resilience.\n\n"
                )
            }
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(
                    "The role of Trusted Approvers\n"
                )
            }
            withStyle(basicStyle) {
                append(
                    "Your Trusted Approvers are your safety net. They could be close family, friends, or even your attorney – people you trust implicitly. But here’s the catch: they can help you access your seed phrase, yet they can't access it themselves. They are your guardians, not gatekeepers.\n\n"
                )
            }
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(
                    "Simple for them, Secure for you\n"
                )
            }
            withStyle(basicStyle) {
                append(
                    "Trusted Approvers don’t need to be crypto gurus. If they can use a smartphone, they can be your rock-solid backup. And for them, it's as easy as accepting a request and following a few simple steps.\n\n"
                )
            }
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(
                    "The Power of Redundancy and No Single Point of Failure\n"
                )
            }
            withStyle(basicStyle) {
                append(
                    "Our 2-of-3 threshold ensures there's no single point of failure. If one Trusted Approver is unavailable or loses their credentials, you still have a backup. It's a seamless blend of security and convenience.\n\n"
                )
            }
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(
                    "A Fail-Safe for Authentication Too\n"
                )
            }
            withStyle(basicStyle) {
                append(
                    "In the rare event you lose access to your usual authentication methods (like your Apple or Google Login ID), your Trusted Approvers can step in. Think of it as a triple-layered safety net."
                )
            }
        }
    }

    fun faceScanMessage(): AnnotatedString {
        val basicStyle = SpanStyle(
            color = SharedColors.MainColorText,
            fontSize = 18.sp
        )

        return buildAnnotatedString {
            withStyle(basicStyle) {
                append(
                    "Censo uses a face scan to ensure your security and protect your privacy. Any security actions you take with Censo require a face scan as one of the sources of authentication.\n\nCenso utilizes face technology built by facetec.com. Facetec’s certified liveness plus 3D face matching ensures that you and only you can access your seed phrases and make changes to your security. Facetec provides over 2 billion 3D liveness checks annually.\n\nBy utilizing Facetec rather than the biometrics on your mobile device, we can assure that you’ll never lose access to your seed phrases, even in the event you lose your mobile device or change the biometry on your phone.\n\nCenso maintains only an encrypted version of your face scan that can never be used to identify you or tied to your identity, although it does allow Censo to positively identify you as a user"
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LearnMoreLabelPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.White)) {
        LearnMoreUI {

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLearnMoreTrustedApproversUI() {
    LearnMoreScreen(
        title = "Trusted Approvers & Safety in Numbers",
        annotatedString = LearnMoreUtil.trustedApproversMessage()
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLearnMoreFaceScanUI() {
    LearnMoreScreen(
        title = "Face Scan & Privacy",
        annotatedString = LearnMoreUtil.faceScanMessage()
    )
}
