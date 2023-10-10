package co.censo.guardian.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.guardian.R
import co.censo.guardian.presentation.GuardianColors
import co.censo.guardian.presentation.home.GuardianHomeViewModel.Companion.VALID_CODE_LENGTH
import co.censo.shared.data.Resource
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.SharedColors.ErrorRed

@Composable
fun ApproverCodeVerification(
    isLoading: Boolean,
    errorResource: Resource.Error<SubmitGuardianVerificationApiResponse>?,
    value: String,
    label: String,
    onValueChanged: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        //Text
        Text(
            modifier = Modifier.padding(16.dp),
            text = label,
            color = GuardianColors.PrimaryColor,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(30.dp))

        //Code entry boxes
        CodeEntry(
            validCodeLength = VALID_CODE_LENGTH,
            isLoading = isLoading,
            value = value,
            onValueChange = onValueChanged
        )
        Spacer(modifier = Modifier.height(36.dp))

        if (isLoading) {
            Text(
                text = stringResource(R.string.waiting_for_owner_verify_code),
                color = SharedColors.GreyText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }

        if (errorResource != null) {
            Text(text = errorResource.getErrorMessage(context = context), color = ErrorRed)
        }
    }
}