package co.censo.shared.data.repository

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.storage.CloudStorage
import org.bouncycastle.util.encoders.Hex
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.projectLog
import co.censo.shared.util.sendError
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
    fun retrieveSavedDeviceId(): String
    fun retrieveInternalDeviceKey(): InternalDeviceKey
    fun createApproverKey(): EncryptionKey
    fun encryptWithDeviceKey(data: ByteArray) : ByteArray
    fun decryptWithDeviceKey(data: ByteArray) : ByteArray
    suspend fun saveKeyInCloud(
        key: ByteArray,
        participantId: ParticipantId,
        bypassScopeCheck: Boolean = false,
        onRetryAfterAccessGranted: () -> Unit
    ) : Resource<Unit>
    suspend fun retrieveKeyFromCloud(
        participantId: ParticipantId,
        bypassScopeCheck: Boolean = false,
        onRetryAfterAccessGranted: () -> Unit
    ): Resource<ByteArray>
    suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean
    suspend fun deleteSavedKeyFromCloud(participantId: ParticipantId): Resource<Unit>
    suspend fun deleteDeviceKeyIfPresent(keyId: String)

    //region CloudAccess
    fun updateCloudAccessState(cloudAccessState: CloudAccessState)

    suspend fun collectCloudAccessState(collector: FlowCollector<CloudAccessState>)
    //endregion
}

class KeyRepositoryImpl(val storage: SecurePreferences, val cloudStorage: CloudStorage) : KeyRepository {

    //region CloudAccessState Flow
    private val cloudAccessStateFlow = MutableStateFlow<CloudAccessState>(CloudAccessState.Uninitialized)
    override fun updateCloudAccessState(cloudAccessState: CloudAccessState) {
        cloudAccessStateFlow.value = cloudAccessState
    }
    override suspend fun collectCloudAccessState(collector: FlowCollector<CloudAccessState>) {
        cloudAccessStateFlow.collect(collector)
    }
    //endregion

    private val keystoreHelper = KeystoreHelper()
    override fun hasKeyWithId(id: String): Boolean {
        return KeystoreHelper().deviceKeyExists(id)
    }

    override fun createAndSaveKeyWithId(id: String) {
        KeystoreHelper().getOrCreateDeviceKey(id)
        setSavedDeviceId(id)
    }

    override fun setSavedDeviceId(id: String) {
        storage.saveDeviceKeyId(id)
    }

    override fun retrieveSavedDeviceId(): String {
        return storage.retrieveDeviceKeyId()
    }

    override fun retrieveInternalDeviceKey() =
        InternalDeviceKey(storage.retrieveDeviceKeyId())

    override fun createApproverKey(): EncryptionKey {
        val keyPair = ECHelper.createECKeyPair()
        return EncryptionKey(keyPair)
    }

    override fun encryptWithDeviceKey(data: ByteArray) =
        retrieveInternalDeviceKey().encrypt(data)

    override fun decryptWithDeviceKey(data: ByteArray) =
        retrieveInternalDeviceKey().decrypt(data)

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun saveKeyInCloud(
        key: ByteArray,
        participantId: ParticipantId,
        bypassScopeCheck: Boolean,
        onRetryAfterAccessGranted: () -> Unit
    ) : Resource<Unit> {
        if (!bypassScopeCheck) {
            if (!cloudStorage.checkUserGrantedCloudStoragePermission()) {
                projectLog(message = "saveKeyInCloud keyRepo: Updating cloud access state for AccessRequired")
                updateCloudAccessState(CloudAccessState.AccessRequired(onRetryAfterAccessGranted))
                projectLog(message = "saveKeyInCloud keyRepo: throwing exception")
                throw CloudStoragePermissionNotGrantedException()
            }
        }

        val response = try {
            cloudStorage.uploadFile(
                fileContent = key.toHexString(),
                participantId = participantId,
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.CloudUpload)
            Resource.Error(exception = e)
        }

        if (response is Resource.Error && response.exception is UserRecoverableAuthIOException) {
            projectLog(message = "saveKeyInCloud keyRepo: Caught UserRecoverableAuthIOException")
            updateCloudAccessState(CloudAccessState.AccessRequired(onRetryAfterAccessGranted))
            projectLog(message = "saveKeyInCloud keyRepo: throwing exception")
            throw CloudStoragePermissionNotGrantedException()
        }

        return response
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun retrieveKeyFromCloud(
        participantId: ParticipantId,
        bypassScopeCheck: Boolean,
        onRetryAfterAccessGranted: () -> Unit
    ): Resource<ByteArray> {
        if (!bypassScopeCheck) {
            if (!cloudStorage.checkUserGrantedCloudStoragePermission()) {
                projectLog(message = "retrieveKeyFromCloud keyRepo: Updating cloud access state for AccessRequired")
                updateCloudAccessState(CloudAccessState.AccessRequired(onRetryAfterAccessGranted))
                projectLog(message = "retrieveKeyFromCloud keyRepo: throwing exception")
                throw CloudStoragePermissionNotGrantedException()
            }
        }

        val response = try {
            cloudStorage.retrieveFileContents(participantId)
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.CloudDownload)
            Resource.Error(exception = e)
        }

        if (response is Resource.Success) {
            val key = Hex.decode(response.data)

            if (key.isEmpty()) {
                val e = Exception("Retrieved private key was empty")
                e.sendError(CrashReportingUtil.CloudDownload)
                return Resource.Error(exception = e)
            }

            return Resource.Success(key)
        } else {

            if (response is Resource.Error && response.exception is UserRecoverableAuthIOException) {
                projectLog(message = "retrieveKeyFromCloud keyRepo: Caught UserRecoverableAuthIOException")
                updateCloudAccessState(CloudAccessState.AccessRequired(onRetryAfterAccessGranted))
                projectLog(message = "retrieveKeyFromCloud keyRepo: throwing exception")
                throw CloudStoragePermissionNotGrantedException()
            }

            return Resource.Error(exception = response.error()?.exception)
        }
    }

    /**
     * Bypass scope check by default,
     * if the user has not granted permission for GDrive access, there is no key to check for
     */
    override suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean {
        val downloadResource = retrieveKeyFromCloud(participantId, bypassScopeCheck = true, onRetryAfterAccessGranted = {})
        return if (downloadResource is Resource.Success) {
            downloadResource.data.isNotEmpty()
        } else {
            false
        }
    }

    override suspend fun deleteSavedKeyFromCloud(participantId: ParticipantId): Resource<Unit> {
        return cloudStorage.deleteFile(participantId)
    }

    override suspend fun deleteDeviceKeyIfPresent(keyId: String) {
        KeystoreHelper().deleteDeviceKeyIfPresent(keyId)
    }
}