package co.censo.shared.presentation.cloud_storage

import Base58EncodedPrivateKey
import ParticipantId
import co.censo.shared.data.Resource

data class CloudStorageHandlerState(
    val cloudActionToPerform: CloudStorageActions = CloudStorageActions.UNINITIALIZED,
    val cloudStorageHandlerArgs: CloudStorageHandlerArgs = CloudStorageHandlerArgs(),
    val shouldEnforceCloudStorageAccess: Boolean = false,
    val cloudStorageActionResource: Resource<ByteArray> = Resource.Uninitialized,
    val cloudStorageAccessGranted: Boolean = false,
)

data class CloudStorageHandlerArgs(
    val participantId: ParticipantId = ParticipantId(""),
    val encryptedPrivateKey: ByteArray? = null,
)

enum class CloudStorageActions {
    UNINITIALIZED, UPLOAD, DOWNLOAD, ENFORCE_ACCESS
}

data class CloudStorageActionData(
    val triggerAction: Boolean = false,
    val action: CloudStorageActions = CloudStorageActions.UNINITIALIZED,
    val reason: Any? = null
)