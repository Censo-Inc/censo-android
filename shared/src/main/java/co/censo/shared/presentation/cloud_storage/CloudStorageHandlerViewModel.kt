package co.censo.shared.presentation.cloud_storage

import Base58EncodedPrivateKey
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudStorageHandlerViewModel @Inject constructor(
    private val keyRepository: KeyRepository
) : ViewModel() {

    var state by mutableStateOf(CloudStorageHandlerState())
        private set

    fun onStart(
        actionToPerform: CloudStorageActions,
        participantId: ParticipantId,
        privateKey: Base58EncodedPrivateKey?
    ) {
        state = state.copy(
            cloudActionToPerform = actionToPerform,
            cloudStorageHandlerArgs = CloudStorageHandlerArgs(
                participantId = participantId, privateKey = privateKey
            )
        )

        performAction()
    }

    fun performAction(bypassScopeCheckForCloudStorage: Boolean = false) {
        if (bypassScopeCheckForCloudStorage && state.shouldEnforceCloudStorageAccess) {
            state = state.copy(shouldEnforceCloudStorageAccess = false)
        }

        when (state.cloudActionToPerform) {
            CloudStorageActions.UNINITIALIZED -> {}
            CloudStorageActions.UPLOAD -> savePrivateKeyToCloud(bypassScopeCheckForCloudStorage)
            CloudStorageActions.DOWNLOAD -> loadPrivateKeyFromCloud(bypassScopeCheckForCloudStorage)
            CloudStorageActions.ENFORCE_ACCESS -> enforceCloudStorageAccess(
                userGrantedCloudStorageAccess = bypassScopeCheckForCloudStorage
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
    ) {
        val privateKey = state.cloudStorageHandlerArgs.privateKey
        val participantId = state.cloudStorageHandlerArgs.participantId

        if (privateKey == null || privateKey.value.isEmpty()) {
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = Exception(
                        "Attempted to save private key but private key was null or empty"
                    )
                )
            )
            return
        }

        if (participantId.value.isEmpty()) {
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = Exception(
                        "Attempted to save private key with empty participantID value"
                    )
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            state = try {
                val uploadResource = keyRepository.saveKeyInCloud(
                    key = privateKey,
                    participantId = participantId,
                    bypassScopeCheck = bypassScopeCheckForCloudStorage
                )

                if (uploadResource is Resource.Success) {
                    state.copy(cloudStorageActionResource = Resource.Success(privateKey))
                } else {
                    state.copy(cloudStorageActionResource = Resource.Error(exception = uploadResource.exception))
                }
            } catch (e: CloudStoragePermissionNotGrantedException) {
                state.copy(shouldEnforceCloudStorageAccess = true)
            }
        }
    }

    private fun loadPrivateKeyFromCloud(
        bypassScopeCheckForCloudStorage: Boolean = false,
    ) {
        val participantId = state.cloudStorageHandlerArgs.participantId
        if (participantId.value.isEmpty()) {
            state = state.copy(
                cloudStorageActionResource = Resource.Error(
                    exception = Exception(
                        "Attempted to load private key with empty participantID value"
                    )
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            state = try {
                val downloadResource = keyRepository.retrieveKeyFromCloud(
                    participantId = participantId,
                    bypassScopeCheck = bypassScopeCheckForCloudStorage
                )

                if (downloadResource is Resource.Success) {
                    val key = downloadResource.data
                    if (key == null || key.value.isEmpty()) {
                        state = state.copy(
                            cloudStorageActionResource = Resource.Error(
                                exception = Exception(
                                    "Retrieved private key was null or empty"
                                )
                            )
                        )
                        return@launch
                    }

                    state.copy(cloudStorageActionResource = Resource.Success(key))
                } else {
                    state.copy(cloudStorageActionResource = Resource.Error(exception = downloadResource.exception))
                }
            } catch (e: CloudStoragePermissionNotGrantedException) {
                state.copy(shouldEnforceCloudStorageAccess = true)
            }
        }
    }

    fun onDispose() {
        state = CloudStorageHandlerState()
    }
}