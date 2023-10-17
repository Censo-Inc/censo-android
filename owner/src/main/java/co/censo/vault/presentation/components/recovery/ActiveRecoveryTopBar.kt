package co.censo.vault.presentation.components.recovery

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryId
import co.censo.shared.data.model.RecoveryStatus
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Composable
fun ActiveRecoveryTopBar(
    recovery: Recovery.ThisDevice,
    approvalsCollected: Int,
    approvalsRequired: Int,
    onCancelRecovery: () -> Unit,
) {

    Column(
        Modifier.background(color = VaultColors.PrimaryColor),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.recovery_initiated),
            fontSize = 36.sp,
            fontWeight = FontWeight.W700,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        RecoveryExpirationCountDown(expiresAt = recovery.expiresAt) {
            onCancelRecovery()
        }

        Spacer(modifier = Modifier.height(8.dp))

        StandardButton(
            modifier = Modifier.padding(horizontal = 72.dp),
            color = VaultColors.PrimaryColor,
            borderColor = Color.White,
            border = true,
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 20.dp),
            onClick = onCancelRecovery,
        ) {
            Text(
                text = stringResource(R.string.cancel_recovery),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        RecoveryApprovalsCollected(
            collected = approvalsCollected,
            required = approvalsRequired
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.required_approvals_reached_to_complete_recovery),
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 30.dp)
        )
    }
}

@Preview
@Composable
fun ActiveRecoveryTopBarPreview() {
    ActiveRecoveryTopBar(
        recovery = Recovery.ThisDevice(
            guid = RecoveryId("recovery id"),
            status = RecoveryStatus.Timelocked,
            createdAt = Clock.System.now(),
            unlocksAt = Clock.System.now() + 5.minutes,
            expiresAt = Clock.System.now() + 1.days,
            approvals = listOf(),
            vaultSecretIds = listOf()
        ),
        approvalsCollected = 2,
        approvalsRequired = 2,
        onCancelRecovery = {}
    )
}