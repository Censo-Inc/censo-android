package co.censo.shared.presentation.cloud_storage

import Base58EncodedPrivateKey
import ParticipantId
import co.censo.shared.data.Resource

data class CloudStorageHandlerState(
    val cloudActionToPerform: CloudStorageAction = CloudStorageAction.Uninitialized,
    val shouldEnforceCloudStorageAccess: Boolean = false,
    val cloudStorageActionResource: Resource<ByteArray> = Resource.Uninitialized,
    val cloudStorageAccessGranted: Boolean = false,
)

data class CloudStorageActionData(
    val triggerAction: Boolean = false,
    val action: CloudStorageAction = CloudStorageAction.Uninitialized
)

sealed class CloudStorageAction {
    data class Upload(
        val participantId: ParticipantId, val encrpytedPrivateKey: ByteArray
    ) : CloudStorageAction()

    data class Download(val participantId: ParticipantId) : CloudStorageAction()
    data object EnforceAccess : CloudStorageAction()
    data object Uninitialized : CloudStorageAction()
}
