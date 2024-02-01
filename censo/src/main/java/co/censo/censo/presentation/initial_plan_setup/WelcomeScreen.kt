package co.censo.censo.presentation.initial_plan_setup

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.components.LargeLoading

@Composable
fun WelcomeScreenUI(
    isPromoCodeEnabled: Boolean,
    showPromoCodeUI: () -> Unit,
    onMainButtonClick: () -> Unit,
    onMinorButtonClick: () -> Unit,
) {
    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.welcome_to_censo),
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                color = Color.Black,
                lineHeight = 48.sp,
                modifier = Modifier
                    .padding(start = 32.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
                    .fillMaxWidth()
            )
            Text(
                stringResource(id = R.string.welcome_blurb),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.google),
                heading = stringResource(R.string.authenticate_privately),
                content = stringResource(R.string.authenticate_privately_blurb),
                completionText = stringResource(R.string.authenticated)
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.small_face_scan),
                heading = stringResource(id = R.string.scan_your_face),
                content = stringResource(id = R.string.scan_your_face_blurb),
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
                heading = stringResource(id = R.string.enter_your_seed_phrase),
                content = stringResource(id = R.string.enter_your_phrase_blurb),
            )
        }

        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (isPromoCodeEnabled) {
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onClick = { showPromoCodeUI() },
                ) {
                    Text(
                        text = stringResource(R.string.have_a_promo_code),
                        style = ButtonTextStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(all = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            StandardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp, vertical = 24.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                onClick = onMainButtonClick,
            ) {
                Text(
                    text = stringResource(id = R.string.secure_your_seed_phrases),
                    style = ButtonTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(all = 8.dp)
                )
            }
            ClickableText(
                text = buildAnnotatedString { append("To become a beneficiary, tap here.") },
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp
                ),
                onClick = { onMinorButtonClick() }
            )
        }
    }
}

@Composable
fun SetupStep(
    imagePainter: Painter,
    heading: String,
    content: String,
    imageBackgroundColor: Color = SharedColors.BackgroundGrey,
    iconColor: Color = Color.Black,
    completionText: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(color = imageBackgroundColor, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Icon(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp),
                tint = iconColor
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = heading,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 16.0.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (completionText != null) {
                Text(
                    text = "✓ $completionText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedColors.SuccessGreen,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPromoCodeUI(
    loading: Boolean,
    inputtedPromoCode: String,
    updatePromoCode: (String) -> Unit,
    submitPromoCode: () -> Unit,
    dismissPromoCodeUI: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    val textFieldStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.W500,
        color = SharedColors.MainColorText,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.5f))
            .clickable { dismissPromoCodeUI() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .padding(horizontal = 44.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = inputtedPromoCode,
                onValueChange = updatePromoCode,
                shape = CircleShape,
                placeholder = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.enter_promo_code),
                        fontSize = textFieldStyle.fontSize,
                        fontWeight = textFieldStyle.fontWeight,
                        textAlign = TextAlign.Center,
                        color = SharedColors.PlaceholderTextGrey,
                    )
                },
                textStyle = textFieldStyle,
                enabled = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = SharedColors.BorderGrey,
                    unfocusedBorderColor = SharedColors.BorderGrey,
                    cursorColor = SharedColors.MainColorText
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = submitPromoCode,
            ) {
                Text(
                    text = stringResource(id = R.string.submit),
                    style = textFieldStyle.copy(color = SharedColors.ButtonTextBlue),
                )
            }
        }
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LargeLoading(fullscreen = false)
        }
    }
}

@Preview
@Composable
fun PreviewEnterPromoCodeUI() {
    EnterPromoCodeUI(
        inputtedPromoCode = "KLJBHVG6757GJH",
        loading = false,
        updatePromoCode = {

        },
        dismissPromoCodeUI = {},
        submitPromoCode = {

        }
    )
}

@Preview
@Composable
fun PreviewWelcomeAndPromoTogether() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Red)
    ) {
        WelcomeScreenUI(
            isPromoCodeEnabled = true,
            showPromoCodeUI = {},
            onMainButtonClick = {}) {

        }

        EnterPromoCodeUI(
            loading = false, inputtedPromoCode = "LKJBH4467FHJGFGH", updatePromoCode = {

            },
            dismissPromoCodeUI = {},
            submitPromoCode = {}
        )
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallWelcomeScreenUIPreview() {
    WelcomeScreenUI(
        isPromoCodeEnabled = true,
        showPromoCodeUI = {},
        onMainButtonClick = {}) {

    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumWelcomeScreenUIPreview() {
    WelcomeScreenUI(
        isPromoCodeEnabled = true,
        showPromoCodeUI = {},
        onMainButtonClick = {}) {

    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeWelcomeScreenUIPreview() {
    WelcomeScreenUI(
        isPromoCodeEnabled = true,
        showPromoCodeUI = {},
        onMainButtonClick = {}) {

    }
}