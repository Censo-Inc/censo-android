package co.censo.censo.presentation.components

import StandardButton
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import co.censo.censo.presentation.onboarding.OnboardingTopBar
import co.censo.shared.R
import co.censo.shared.data.model.termsOfUseVersions
import co.censo.shared.data.model.touVersion
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TermsOfUse(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    onboarding: Boolean
) {

    var isReview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OnboardingTopBar(onCancel = onCancel, title = "Terms of Use", onboarding = onboarding)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isReview) {
                HtmlText(
                    termsOfUseVersions[touVersion]!!,
                    Modifier
                        .padding(paddingValues)
                        .fillMaxHeight(0.8f)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Image(
                    painterResource(id = R.drawable.files),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = SharedColors.LoginIconColor)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.terms_of_use),
                    fontWeight = FontWeight.W600,
                    fontSize = 24.sp,
                    color = SharedColors.MainColorText
                )
                Spacer(modifier = Modifier.height(24.dp))
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onClick = { isReview = true },
                ) {
                    Text(
                        text = stringResource(id = R.string.tou_review),
                        style = ButtonTextStyle.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(all = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onAccept,
            ) {
                Text(
                    text = stringResource(R.string.tou_accept),
                    modifier = Modifier.padding(all = 8.dp),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.tou_agreement),
                fontSize = 11.sp,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, @ColorInt color: Int = android.graphics.Color.parseColor("#000000")) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context).apply {
            setTextColor(color)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } },
        update = {
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun TermsOfUsePreview() {
    TermsOfUse(onAccept =  {
        print("Accepted!")
    }, onCancel = {}, true)
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun TermsOfUsePreviewNotOnboarding() {
    TermsOfUse(onAccept =  {
        print("Accepted!")
    }, onCancel = {}, false)
}