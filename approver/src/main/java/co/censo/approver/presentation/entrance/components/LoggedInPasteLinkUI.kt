package co.censo.approver.presentation.entrance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import co.censo.approver.R
import co.censo.approver.presentation.components.PasteLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedInPasteLinkUI(
    isApprover: Boolean,
    onPasteLinkClick: () -> Unit,
    onAssistClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                    )
                }
            },
            title = {}
        )
    }) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .background(color = Color.White)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PasteLink(
                    isApprover = isApprover,
                    onPasteLinkClick = onPasteLinkClick,
                    onAssistClick = onAssistClick
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoggedInPasteLinkUIPreview() {
    LoggedInPasteLinkUI(
        isApprover = true,
        onPasteLinkClick = {},
        onAssistClick = {},
        onBackClick = {},
    )
}
