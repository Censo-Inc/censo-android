package co.censo.vault.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.shared.data.Resource
import co.censo.vault.presentation.components.OnLifecycleEvent
import co.censo.vault.ui.theme.DialogMainBackground
import co.censo.vault.ui.theme.TextBlack
import co.censo.vault.ui.theme.UnfocusedGrey
import co.censo.vault.util.TestTag
import co.censo.vault.util.vaultLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController, viewModel: HomeViewModel = hiltViewModel()
) {

    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    fun checkPermissionDialog() {
        try {
            val notificationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            val shownPermissionJustOnceBefore =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            val seenDialogBefore = viewModel.userHasSeenPushDialog()

            if (notificationGranted != PackageManager.PERMISSION_GRANTED) {
                if (shownPermissionJustOnceBefore && !seenDialogBefore) {
                    viewModel.setUserSeenPushDialog(true)
                    viewModel.triggerPushNotificationDialog()
                } else if (!seenDialogBefore) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        } catch (e: Exception) {
            vaultLog(message = "checkPermissionDialog exception caught: ${e.message}")
            //TODO: Log exception
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                checkPermissionDialog()
            }
            else -> Unit
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    Scaffold(
        modifier = Modifier.semantics {
            testTag = TestTag.home_screen_container
        },
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = TestTag.home_screen_app_bar },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text = stringResource(R.string.your_bip39_phrases))
                    }
                })
        },
        content = {
            Box(modifier = Modifier.padding(it).fillMaxSize().background(color = Color.White)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .background(color = Color.White)
                        .padding(horizontal = 16.dp)
                        .semantics { testTag = TestTag.phrases_list },
                ) {
                    state.phrases.forEach { phrase ->
                        ClickableText(
                            modifier = Modifier.padding(all = 48.dp),
                            text = AnnotatedString(text = phrase.key),
                            onClick = {
                                navController.navigate(
                                    "${Screen.BIP39DetailRoute.route}/${phrase.key}"
                                )
                            }
                        )
                    }
                }
            }

            if (state.showPushNotificationsDialog is Resource.Success) {
                PushNotificationDialog(
                    text = stringResource(id = R.string.push_notification_never_dialog),
                    onAccept = {
                        viewModel.resetPushNotificationDialog()
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onDismiss = {
                        viewModel.setUserSeenPushDialog(false)
                        viewModel.resetPushNotificationDialog()
                    }
                )
            }

        },
        floatingActionButton = {

            Column {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.GuardianInvitationRoute.route)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = "Guardians",
                        tint = Color.White,
                    )
                }
                
                Spacer(modifier = Modifier.height(56.dp))

                FloatingActionButton(
                    modifier = Modifier.semantics { testTag = TestTag.add_bip39_button },
                    onClick = {
                        navController.navigate(Screen.AddBIP39Route.route)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_bip39_phrase),
                        tint = Color.White,
                    )
                }
            }
        })
}

@Composable
fun PushNotificationDialog(
    text: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = UnfocusedGrey.copy(alpha = 0.50f))
                .background(color = DialogMainBackground)
                .shadow(elevation = 2.5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = text,
                textAlign = TextAlign.Center,
                color = TextBlack,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Row {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.skip),
                        fontSize = 18.sp,
                        color = TextBlack,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                    onClick = onAccept,
                ) {
                    Text(
                        text = stringResource(id = R.string.continue_text),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}