package co.censo.approver.presentation.entrance.components

import StandardButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApproverLoginUI(
    authenticate: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = co.censo.shared.R.drawable.logo),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(id = co.censo.shared.R.drawable.censo_text),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(co.censo.shared.R.string.sensible_crypto_security),
            fontWeight = FontWeight.W600,
            fontSize = 24.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        StandardButton(
            onClick = authenticate,
            contentPadding = PaddingValues(
                horizontal = 48.dp,
                vertical = 16.dp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
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
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(co.censo.shared.R.string.google_auth_login),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = 10.dp)
        ) {
            Icon(
                Icons.Outlined.Info, contentDescription = null,
                Modifier
                    .height(height = 34.dp)
                    .padding(all = 8.dp)
            )
            Text(
                text = stringResource(co.censo.shared.R.string.why_google),
                fontSize = 20.sp,
                fontWeight = FontWeight.W500
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(0.1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = co.censo.shared.R.drawable.eyeslash),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(co.censo.shared.R.string.no_personal_info),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = co.censo.shared.R.drawable.safe),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(co.censo.shared.R.string.multiple_layers),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
        Divider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        Row(
            modifier = Modifier.padding(
                start = 32.dp,
                end = 32.dp,
                top = 20.dp,
                bottom = 32.dp
            )
        ) {
            Text(
                text = stringResource(co.censo.shared.R.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/terms/") },
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = stringResource(co.censo.shared.R.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/privacy/") },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview()
@Composable
fun ApproverLoginUIPreview() {
    Surface {
        ApproverLoginUI(
            authenticate = {}
        )
    }
}
