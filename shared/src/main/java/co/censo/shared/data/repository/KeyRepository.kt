package co.censo.shared.data.repository

import Base58EncodedPrivateKey
import ParticipantId
import android.content.Context
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.storage.CloudStorage
import co.censo.shared.data.storage.Storage

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
    fun retrieveInternalDeviceKey(): InternalDeviceKey
    fun createGuardianKey(): EncryptionKey
    fun encryptWithDeviceKey(data: ByteArray) : ByteArray
    fun decryptWithDeviceKey(data: ByteArray) : ByteArray
    suspend fun saveKeyInCloud(
        key: Base58EncodedPrivateKey,
        context: Context,
        appName: String,
        participantId: ParticipantId
    )
    suspend fun retrieveKeyFromCloud(participantId: ParticipantId): Base58EncodedPrivateKey
    suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean
}

class KeyRepositoryImpl(val storage: Storage, val cloudStorage: CloudStorage) : KeyRepository {

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

    override fun retrieveInternalDeviceKey() =
        InternalDeviceKey(storage.retrieveDeviceKeyId())

    override fun createGuardianKey(): EncryptionKey {
        val keyPair = ECHelper.createECKeyPair()
        return EncryptionKey(keyPair)
    }

    override fun encryptWithDeviceKey(data: ByteArray) =
        retrieveInternalDeviceKey().encrypt(data)

    override fun decryptWithDeviceKey(data: ByteArray) =
        retrieveInternalDeviceKey().decrypt(data)

    override suspend fun saveKeyInCloud(
        key: Base58EncodedPrivateKey,
        context: Context,
        appName: String,
        participantId: ParticipantId
    ) {
        //TODO: Save to sharedPrefs for now until Google Drive File Retrieval is in
        storage.savePrivateKey(key = key.value)

//        cloudStorage.uploadFile(
//            fileContent = key.value,
//            participantId = participantId,
//            appName = appName
//        )
    }

    override suspend fun retrieveKeyFromCloud(participantId: ParticipantId): Base58EncodedPrivateKey {
        //TODO: Retrieve from sharedPrefs for now until Google Drive File Retrieval is in
        return Base58EncodedPrivateKey(storage.retrievePrivateKey())
    }

    override suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean {
        return retrieveKeyFromCloud(participantId).value.isNotEmpty()
    }
}