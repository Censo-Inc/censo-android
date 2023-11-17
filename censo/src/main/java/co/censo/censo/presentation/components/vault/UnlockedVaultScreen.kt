package co.censo.censo.presentation.components.vault

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors

@Composable
fun UnlockedVaultScreen(
    onEditSeedPhrases: () -> Unit,
    onRecoverSeedPhrases: () -> Unit,
    onResetUser: () -> Unit,
    showAddApprovers: Boolean
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.your_seed_phrases_are_protected),
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.W700
        )

        Spacer(modifier = Modifier.weight(1f))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = VaultColors.PrimaryColor,
            borderColor = VaultColors.PrimaryColor,
            border = true,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
            onClick = onEditSeedPhrases,
        ) {
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.manual_entry_icon),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.add_seed_phrases),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = VaultColors.PrimaryColor,
            borderColor = VaultColors.PrimaryColor,
            border = true,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
            onClick = onRecoverSeedPhrases,
        ) {
            Row {
                Icon(
                    painter = painterResource(id = co.censo.shared.R.drawable.key),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(PaddingValues(vertical = 3.dp))
                )
                Spacer(modifier = Modifier.width(28.dp))
                Text(
                    text = stringResource(R.string.access_phrases),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showAddApprovers) {
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                color = VaultColors.PrimaryColor,
                borderColor = VaultColors.PrimaryColor,
                border = true,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                onClick = {},
            ) {
                Row {
                    Icon(
                        painter = painterResource(id = co.censo.shared.R.drawable.two_people),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(PaddingValues(vertical = 3.dp))
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = stringResource(R.string.invite_approvers),
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = VaultColors.PrimaryColor,
            borderColor = VaultColors.PrimaryColor,
            border = true,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
            onClick = onResetUser,
        ) {
            Row {
                Icon(
                    painter = painterResource(id = co.censo.shared.R.drawable.reset),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(PaddingValues(vertical = 3.dp))
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = stringResource(R.string.reset_user_data),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

    }
}

@Preview
@Composable
fun UnlockedVaultScreenPreview() {
    UnlockedVaultScreen(
        onEditSeedPhrases = {},
        onRecoverSeedPhrases = {},
        onResetUser = {},
        showAddApprovers = true
    )
}