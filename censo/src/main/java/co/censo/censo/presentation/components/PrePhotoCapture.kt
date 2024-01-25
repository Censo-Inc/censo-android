package co.censo.censo.presentation.components

import StandardButton
import TitleText
import android.Manifest
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.plan_setup.components.ApproverStep
import co.censo.censo.ui.theme.TextBlack
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.Permission
import co.censo.shared.presentation.components.sendUserToPermissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureSeedPhraseImage(
    executor: Executor,
    onImageCaptured: (ImageProxy) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current

    val showCamera = remember { mutableStateOf(false) }
    when (showCamera.value) {
        false -> {
            PreCaptureImageStep {
                showCamera.value = true
            }
        }

        true -> {
            Permission(
                permission = Manifest.permission.CAMERA,
                rationale = "Camera is used to capture a photo of your seed phrase",
                permissionNotAvailableContent = {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.unable_to_access_camera),
                            color = SharedColors.MainColorText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W600,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(R.string.grant_censo_access_to_your_camera_in_order_to_take_your_photo),
                            color = SharedColors.MainColorText,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(R.string.settings_app_direction),
                            color = SharedColors.MainColorText,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        StandardButton(
                            onClick = { context.sendUserToPermissions() },
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.open_settings_app),
                                style = ButtonTextStyle
                            )
                        }
                    }
                }) {
                CameraView(
                    executor = executor,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            }
        }
    }
}

@Composable
fun PreCaptureImageStep(
    onStartTapped: () -> Unit
) {
    val boxBackgroundGrey = Color.Gray.copy(alpha = 0.25f)
    val boxRoundedBackgroundShape = RoundedCornerShape(12.dp)

    //region String spanning
    val basicStyle = SpanStyle(
        color = SharedColors.MainColorText,
        fontSize = 16.sp
    )

    val step2AnnotatedString = buildAnnotatedString {
        withStyle(basicStyle) {
            append(stringResource(R.string.photo_capture_step_2_preceding_text))
        }

        withStyle(style = basicStyle.copy(fontWeight = FontWeight.W600)) {
            append(" ")
            append(stringResource(R.string.step_2_spanned_text))
            append(" ")
        }

        withStyle(basicStyle) {
            append(stringResource(R.string.photo_capture_step_2_remaining_text))
        }
    }
    //endregion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.take_a_photo),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))

        ApproverStep(
            heading = stringResource(R.string._1_prepare),
            content = stringResource(R.string.seed_phrase_photo_step_1_content),
            contentFontSize = 16.sp,
            includeLine = false,
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(color = boxBackgroundGrey, shape = boxRoundedBackgroundShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.size(30.dp),
                    imageVector = Icons.Filled.Notes,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            heading = stringResource(R.string._2_take_a_photo_of_it),
            content = step2AnnotatedString,
            contentFontSize = 16.sp,
            includeLine = false,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(color = boxBackgroundGrey, shape = boxRoundedBackgroundShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.size(30.dp),
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onStartTapped
        ) {
            Text(
                text = stringResource(R.string.start),
                style = ButtonTextStyle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}