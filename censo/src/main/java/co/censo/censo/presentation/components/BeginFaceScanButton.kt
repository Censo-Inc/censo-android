package co.censo.censo.presentation.components

import StandardButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun BeginFaceScanButton(spacing: Dp, onBeginFaceScan: () -> Unit) {
    Text(
        text = stringResource(R.string.affirmative_biometric_consent),
        fontWeight = FontWeight.Light,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = SharedColors.MainColorText,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(spacing))
    StandardButton(
        onClick = onBeginFaceScan,
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = co.censo.shared.R.drawable.small_face_scan_white),
                contentDescription = null,
                modifier = Modifier.width(32.dp),
                colorFilter = ColorFilter.tint(SharedColors.ButtonTextBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.begin_face_scan),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}