package co.censo.shared.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicScreen(
    loading: Boolean,
    error: String?,
    resetError: () -> Unit,
    retry: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold {
        Box(modifier = androidx.compose.ui.Modifier.padding(it)) {
            content(it)
        }

        if (error != null) {
            DisplayError(
                errorMessage = error,
                dismissAction = resetError,
                retryAction = retry
            )
        }

        if (loading) {
            LargeLoading(fullscreen = true)
        }
    }
}