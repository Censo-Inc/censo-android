import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import co.censo.vault.R
import co.censo.vault.Resource
import co.censo.vault.TestTag
import co.censo.vault.presentation.main.BlockAppUI
import co.censo.vault.presentation.components.VaultButton
import javax.crypto.Cipher

@Composable
fun BlockingUI(
    blockAppUI: BlockAppUI,
    bioPromptTrigger: Resource<Unit>,
    biometryUnavailable: Boolean,
    biometryStatus: BiometricUtil.Companion.BiometricsStatus?,
    retry: () -> Unit
) {
    when (blockAppUI) {
        BlockAppUI.BIOMETRY_DISABLED -> {
            DisabledBiometryUI(
                biometryStatus
            )
        }
        BlockAppUI.FOREGROUND_BIOMETRY -> {
            ForegroundBlockingUI(
                bioPromptTrigger = bioPromptTrigger,
                biometryUnavailable = biometryUnavailable,
                retry = retry
            )
        }
        BlockAppUI.NONE -> {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@Composable
fun ForegroundBlockingUI(
    bioPromptTrigger: Resource<Unit>,
    biometryUnavailable: Boolean,
    retry: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = Color.Gray)
            .semantics { testTag = TestTag.biometry_blocking_ui_container }
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) { },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 124.dp, start = 48.dp, end = 48.dp),
                text = if (biometryUnavailable)
                    stringResource(R.string.biometry_unavailable)
                else stringResource(R.string.foreground_access_app),
                fontSize = 24.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (bioPromptTrigger is Resource.Error) {
                VaultButton(
                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 10.dp),
                    onClick = retry
                ) {
                    Text(
                        text = stringResource(R.string.try_again),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DisabledBiometryUI(biometryStatus: BiometricUtil.Companion.BiometricsStatus?) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) { },
    ) {
        val message: Int =
            if (biometryStatus == BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_DISABLED) {
                R.string.biometry_disabled_message
            } else {
                R.string.biometry_unavailable_message
            }

        BiometryDisabledScreen(
            message = stringResource(id = message),
            biometryAvailable = biometryStatus != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_NOT_AVAILABLE
        )
    }
}

@Composable
fun BiometryDisabledScreen(message: String, biometryAvailable: Boolean) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(24.dp),
            text = message,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
        if (biometryAvailable) {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)

            Spacer(modifier = Modifier.height(32.dp))
            VaultButton(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 4.dp),
                onClick = {
                    try {
                        ContextCompat.startActivity(context, intent, null)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.update_biometric_settings),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.biometry_deeplink_device_settings),
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}