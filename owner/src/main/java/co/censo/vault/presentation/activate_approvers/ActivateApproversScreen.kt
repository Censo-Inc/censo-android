package co.censo.vault.presentation.activate_approvers

import FullScreenButton
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import co.censo.shared.presentation.Colors
import co.censo.vault.R
import co.censo.vault.presentation.components.ActivateApproverRow
import co.censo.vault.presentation.components.ActivateApproversTopBar
import co.censo.vault.presentation.components.dummyListOfApprovers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivateApproversScreen(
    navController: NavController,
) {

    val approvers = dummyListOfApprovers

    Scaffold(
        contentColor = Color.White,
        containerColor = Color.White,
        topBar = {
            ActivateApproversTopBar()
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FullScreenButton(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Colors.PrimaryBlue,
                    textColor = Color.White,
                    border = false,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = {},
                )
                {
                    Text(
                        text = stringResource(id = R.string.continue_text),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Activate Approvers",
                    fontSize = 24.sp,
                    color = Colors.PrimaryBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                for (approver in approvers) {
                    ActivateApproverRow(approver = approver) {

                    }
                }
            }
        }
    }
}
