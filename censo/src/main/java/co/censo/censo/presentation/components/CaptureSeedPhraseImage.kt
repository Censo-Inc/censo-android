package co.censo.censo.presentation.components

import android.Manifest
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import co.censo.censo.R
import co.censo.censo.presentation.enter_phrase.components.CameraPermissionNotAvailable
import co.censo.censo.presentation.enter_phrase.components.PreCaptureImageStep
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.Permission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureSeedPhraseImage(
    retakingImage: Boolean,
    onImageCaptured: (ImageProxy) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                executor.shutdown()
            }

            else -> Unit
        }
    }

    //If retaking image then show camera immediately
    val showCamera = remember { mutableStateOf(retakingImage) }
    when (showCamera.value) {
        false -> {
            PreCaptureImageStep {
                showCamera.value = true
            }
        }

        true -> {
            Permission(
                permission = Manifest.permission.CAMERA,
                rationale = stringResource(R.string.capture_image_permission_rationale),
                permissionNotAvailableContent = {
                    CameraPermissionNotAvailable()
                }) {
                CameraView(
                    executor = executor,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            }
        }
    }
}