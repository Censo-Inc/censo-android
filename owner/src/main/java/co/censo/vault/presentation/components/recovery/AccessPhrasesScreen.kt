package co.censo.vault.presentation.components.recovery

import FullScreenButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryId
import co.censo.shared.data.model.RecoveryStatus
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Composable
fun AccessPhrasesScreen(
    recovery: Recovery.ThisDevice,
    approvalsCollected: Int,
    approvalsRequired: Int,
    onCancelRecovery: () -> Unit,
    onRecoverPhrases: () -> Unit,
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = VaultColors.PrimaryColor)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        ActiveRecoveryTopBar(
            recovery,
            approvalsCollected,
            approvalsRequired,
            onCancelRecovery,
        )

        Spacer(modifier = Modifier.weight(1f))

        val available = recovery.status == RecoveryStatus.Available

        FullScreenButton(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.White,
            borderColor = Color.White,
            border = false,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onRecoverPhrases,
            enabled = available
        ) {
            Text(
                text = "Access Seed Phrases",
                color = VaultColors.PrimaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700
            )
        }

        if (!available) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Available in a few minutes",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
fun AccessPhrasesScreenPreview() {
    AccessPhrasesScreen(
        recovery = Recovery.ThisDevice(
            guid = RecoveryId("recovery id"),
            status = RecoveryStatus.Timelocked,
            createdAt = Clock.System.now(),
            unlocksAt = Clock.System.now() + 5.minutes,
            expiresAt = Clock.System.now() + 1.days,
            approvals = listOf(),
            vaultSecretIds = listOf()
        ),
        approvalsCollected = 3,
        approvalsRequired = 3,
        onCancelRecovery = { },
        onRecoverPhrases = { }
    )
}