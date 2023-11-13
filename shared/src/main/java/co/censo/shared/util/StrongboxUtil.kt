package co.censo.shared.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.BuildConfig
import co.censo.shared.R


@Composable
fun StrongboxUI() {
    val context = LocalContext.current

    if (!StrongboxUtil.deviceHasStrongbox(context) && BuildConfig.BUILD_TYPE != "debug") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                text = stringResource(R.string.storngbox_incompatible_message),
            )
        }
    } else {
        Spacer(Modifier.size(0.dp))
    }
}

object StrongboxUtil {
    fun deviceHasStrongbox(context: Context): Boolean {

        return context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_STRONGBOX_KEYSTORE
        )
    }
}