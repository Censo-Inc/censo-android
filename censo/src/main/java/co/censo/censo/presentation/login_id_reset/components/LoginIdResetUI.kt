package co.censo.censo.presentation.login_id_reset.components

import StandardButton
import TitleText
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.login_id_reset.LoginIdResetStep
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun LoginIdResetUI(
    resetStep: LoginIdResetStep,
    linksCollected: Int,
    linksRequired: Int,
    onPasteLink: () -> Unit,
    onSelectGoogleId: () -> Unit,
    onFaceScan: () -> Unit,
    onKeyRecovery: () -> Unit,
) {

    val enabledButtonStyle = ButtonTextStyle.copy(fontWeight = null)
    val disabledButtonStyle = DisabledButtonTextStyle.copy(fontWeight = null)

    val pasteButtonEnabled = resetStep == LoginIdResetStep.PasteResetLinks
    val googleIdButtonEnabled = resetStep == LoginIdResetStep.SelectLoginId
    val verificationButtonEnabled = resetStep == LoginIdResetStep.Facetec
    val keyRecoveryButtonEnabled = resetStep == LoginIdResetStep.KeyRecovery

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TitleText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            title = stringResource(R.string.reset_login_id)
        )
        Spacer(modifier = Modifier.height(24.dp))
        BaseResetStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.import_paste),
            heading = stringResource(R.string.step_1_collect_reset_links),
            message = stringResource(R.string.step_1_collect_reset_links_message),
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.collected_reset_links, linksCollected, linksRequired),
                fontSize = 14.sp,
                color = SharedColors.MainColorText
            )
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.85f)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 18.dp),
                onClick = onPasteLink,
                enabled = pasteButtonEnabled
            ) {
                Text(
                    text = stringResource(co.censo.censo.R.string.paste_from_clipboard),
                    style = if (pasteButtonEnabled) enabledButtonStyle else disabledButtonStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        BaseResetStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.google),
            heading = stringResource(R.string.step_2_select_new_google_id),
            message = stringResource(R.string.step_2_select_new_google_id_message)
        ) {
            StandardButton(
                onClick = onSelectGoogleId,
                enabled = googleIdButtonEnabled,
                coolDownDuration = 500.milliseconds,
                color = Color.Black,
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 18.dp),
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.85f)
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = co.censo.shared.R.drawable.google),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(co.censo.shared.R.string.google_auth_login),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        BaseResetStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.small_face_scan),
            heading = stringResource(R.string.step_3_biometric_verification),
            message = stringResource(R.string.step_3_biometric_verification_message)
        ) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.85f)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 18.dp),
                onClick = onFaceScan,
                enabled = verificationButtonEnabled,
            ) {
                Text(
                    text = stringResource(R.string.start_verification),
                    style = if (verificationButtonEnabled) enabledButtonStyle else disabledButtonStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        BaseResetStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.two_people),
            heading = stringResource(R.string.step_4_key_recovery),
            message = stringResource(R.string.step_4_key_recovery_message)
        ) {
            StandardButton(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.85f)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 18.dp),
                onClick = onKeyRecovery,
                enabled = keyRecoveryButtonEnabled,
            ) {
                Text(
                    text =stringResource(R.string.recover_my_key),
                    style = if (keyRecoveryButtonEnabled) enabledButtonStyle else disabledButtonStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Preview
@Composable
fun ResetTokensUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {
        LoginIdResetUI(
            resetStep = LoginIdResetStep.PasteResetLinks,
            linksCollected = 1,
            linksRequired = 2,
            onPasteLink = {},
            onSelectGoogleId = {},
            onFaceScan = {},
            onKeyRecovery = {},
        )
    }
}