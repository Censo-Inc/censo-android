package co.censo.shared.presentation.cloud_storage

import Base58EncodedPrivateKey
import ParticipantId
import co.censo.shared.data.Resource

data class CloudStorageState(
    val cloudActionToPerform: CloudStorageActions = CloudStorageActions.UNINITIALIZED,
    val cloudStorageHandlerArgs: CloudStorageHandlerArgs = CloudStorageHandlerArgs(),
    val shouldEnforceCloudStorageAccess: Boolean = false,
    val cloudStorageActionResource: Resource<Base58EncodedPrivateKey> = Resource.Uninitialized,
    val cloudStorageAccessGranted: Boolean = false,
)

data class CloudStorageHandlerArgs(
    val participantId: ParticipantId = ParticipantId(""),
    val privateKey: Base58EncodedPrivateKey? = null,
)

enum class CloudStorageActions {
    UNINITIALIZED, UPLOAD, DOWNLOAD, ENFORCE_ACCESS
}