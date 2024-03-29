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
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError

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
        bypassScopeCheck: Boolean = false
    ) : Resource<Unit>
    suspend fun retrieveKeyFromCloud(
        participantId: ParticipantId,
        bypassScopeCheck: Boolean = false
    ): Resource<ByteArray>
    suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean
    suspend fun deleteSavedKeyFromCloud(participantId: ParticipantId): Resource<Unit>
    suspend fun deleteDeviceKeyIfPresent(keyId: String)
}

class KeyRepositoryImpl(val storage: SecurePreferences, val cloudStorage: CloudStorage) : KeyRepository {

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
        bypassScopeCheck: Boolean
    ) : Resource<Unit> {
        if (!bypassScopeCheck) {
            if (!cloudStorage.checkUserGrantedCloudStoragePermission()) {
                throw CloudStoragePermissionNotGrantedException()
            }
        }

        return try {
            cloudStorage.uploadFile(
                fileContent = key.toHexString(),
                participantId = participantId,
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.CloudUpload)
            Resource.Error(exception = e)
        }
    }

    /**
     * Can throw a CLOUD_STORAGE_PERMISSION_NOT_GRANTED_EXCEPTION,
     * the caller should wrap this method in a try catch
     */
    override suspend fun retrieveKeyFromCloud(
        participantId: ParticipantId,
        bypassScopeCheck: Boolean
    ): Resource<ByteArray> {
        if (!bypassScopeCheck) {
            if (!cloudStorage.checkUserGrantedCloudStoragePermission()) {
                throw CloudStoragePermissionNotGrantedException()
            }
        }

        return try {
            val resource = cloudStorage.retrieveFileContents(participantId)

            if (resource is Resource.Success) {
                return Resource.Success(Hex.decode(resource.data))
            } else {
                return Resource.Error(exception = resource.error()?.exception)
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.CloudDownload)
            Resource.Error(exception = e)
        }
    }

    /**
     * Bypass scope check by default,
     * if the user has not granted permission for GDrive access, there is no key to check for
     */
    override suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean {
        val downloadResource = retrieveKeyFromCloud(participantId, bypassScopeCheck = true)
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