package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import StandardButton
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.censo.shared.DeepLinkURI

@Composable
fun ShareBeneficiaryInviteUI(
    inviteId: String
) {
    val context = LocalContext.current

    fun shareLink(link: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Column {
        Text(text = "Beneficiary Created")
        Spacer(modifier = Modifier.height(24.dp))
        StandardButton(onClick = {
            shareLink(
                DeepLinkURI.createBeneficiaryDeeplink(inviteId) ?: ""
            )
        }) {
            Text(text = "Share Invitation Link")
        }
    }
}