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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import co.censo.approver.R
import co.censo.shared.util.LinksUtil

@Composable
fun ApproverLoginUI(
    authenticate: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Image(
            modifier = Modifier.padding(horizontal = 12.dp),
            painter = painterResource(id = R.drawable.censo_approver_logo),
            contentDescription = null,
        )

        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                fontSize = 16.sp,
                text = stringResource(R.string.sign_in_google_explainer),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tapping_sign_in),
                color = Color.Black,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier.padding(
                    horizontal = 12.dp, vertical = 8.dp
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 32.dp, vertical = 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    text = stringResource(co.censo.shared.R.string.terms),
                    modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.TERMS_URL) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(56.dp))
                Text(
                    text = stringResource(co.censo.shared.R.string.privacy),
                    modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.PRIVACY_URL) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
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
