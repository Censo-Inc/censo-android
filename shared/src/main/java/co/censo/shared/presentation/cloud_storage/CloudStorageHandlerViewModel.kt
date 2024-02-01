package co.censo.shared.presentation.cloud_storage

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudStorageHandlerViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    var state by mutableStateOf(CloudStorageHandlerState())
        private set

    fun onStart(
        actionToPerform: CloudStorageAction
    ) {
        state = state.copy(cloudActionToPerform = actionToPerform)

        performAction()
    }

    fun performAction(bypassScopeCheckForCloudStorage: Boolean = false) {
        if (bypassScopeCheckForCloudStorage && state.shouldEnforceCloudStorageAccess) {
            state = state.copy(shouldEnforceCloudStorageAccess = false)
        }

        when (val action = state.cloudActionToPerform) {
            CloudStorageAction.Uninitialized -> {}

            CloudStorageAction.EnforceAccess -> enforceCloudStorageAccess(
                userGrantedCloudStorageAccess = bypassScopeCheckForCloudStorage
            )

            is CloudStorageAction.Download -> loadPrivateKeyFromCloud(
                bypassScopeCheckForCloudStorage = bypassScopeCheckForCloudStorage,
                participantId = action.participantId
            )
            is CloudStorageAction.Upload -> savePrivateKeyToCloud(
                bypassScopeCheckForCloudStorage = bypassScopeCheckForCloudStorage,
                participantId = action.participantId,
                encryptedPrivateKey = action.encrpytedPrivateKey
            )
        }
    }

    private fun enforceCloudStorageAccess(userGrantedCloudStorageAccess: Boolean) {
        state = if (userGrantedCloudStorageAccess) {
            state.copy(cloudStorageAccessGranted = true)
        } else {
            state.copy(shouldEnforceCloudStorageAccess = true)
        }
    }

    private fun savePrivateKeyToCloud(
        bypassScopeCheckForCloudStorage: Boolean = false,
        participantId: ParticipantId,
        encryptedPrivateKey: ByteArray
    ) {
        if (encryptedPrivateKey.isEmpty()) {
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = Exception(
                        "Attempted to save private key but private key was empty"
                    )
                )
            )
            return
        }

        if (participantId.value.isEmpty()) {
            val e = Exception("Attempted to save private key with empty participantID value")
            e.sendError(CrashReportingUtil.CloudUpload)
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = e
                )
            )
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val uploadResource = keyRepository.saveKeyInCloud(
                    key = encryptedPrivateKey,
                    participantId = participantId,
                    bypassScopeCheck = bypassScopeCheckForCloudStorage
                )

                if (uploadResource is Resource.Success) {
                    state = state.copy(cloudStorageActionResource = Resource.Success(
                        encryptedPrivateKey
                    ))
                } else if (uploadResource is Resource.Error)  {
                    uploadResource.exception?.sendError(CrashReportingUtil.CloudUpload)
                    state = state.copy(cloudStorageActionResource = Resource.Error(exception = uploadResource.exception))
                }
            } catch (e: CloudStoragePermissionNotGrantedException) {
                e.sendError(CrashReportingUtil.CloudUpload)
                state = state.copy(shouldEnforceCloudStorageAccess = true)
            }
        }
    }

    private fun loadPrivateKeyFromCloud(
        bypassScopeCheckForCloudStorage: Boolean = false,
        participantId: ParticipantId
    ) {
        if (participantId.value.isEmpty()) {
            val e = Exception("Attempted to load private key with empty participantID value")
            e.sendError(CrashReportingUtil.CloudDownload)
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = e
                )
            )
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val downloadResource = keyRepository.retrieveKeyFromCloud(
                    participantId = participantId,
                    bypassScopeCheck = bypassScopeCheckForCloudStorage
                )

                if (downloadResource is Resource.Success) {
                    val key = downloadResource.data
                    if (key.isEmpty()) {
                        val e = Exception("Retrieved private key was empty")
                        e.sendError(CrashReportingUtil.CloudDownload)
                        state = state.copy(
                            cloudStorageActionResource = Resource.Error(
                                exception = e
                            )
                        )
                        return@launch
                    }

                    state = state.copy(cloudStorageActionResource = Resource.Success(key))
                } else if (downloadResource is Resource.Error) {
                    downloadResource.exception?.sendError(CrashReportingUtil.CloudDownload)
                    state = state.copy(cloudStorageActionResource = Resource.Error(exception = downloadResource.exception))
                }
            } catch (e: CloudStoragePermissionNotGrantedException) {
                e.sendError(CrashReportingUtil.CloudDownload)
                state = state.copy(shouldEnforceCloudStorageAccess = true)
            }
        }
    }

    fun onDispose() {
        state = CloudStorageHandlerState()
    }
}