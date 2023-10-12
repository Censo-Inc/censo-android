package co.censo.shared.data.repository

import Base58EncodedPrivateKey
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.storage.Storage

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
    fun retrieveInternalDeviceKey(): InternalDeviceKey
    fun createGuardianKey(): EncryptionKey
    fun encryptWithDeviceKey(data: ByteArray) : ByteArray
    fun decryptWithDeviceKey(data: ByteArray) : ByteArray

    suspend fun saveKeyInCloud(key: Base58EncodedPrivateKey)
    suspend fun retrieveKeyFromCloud(): Base58EncodedPrivateKey
}

class KeyRepositoryImpl(val storage: Storage) : KeyRepository {

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

    override suspend fun saveKeyInCloud(key: Base58EncodedPrivateKey) {
        storage.savePrivateKey(key.value)
    }
    override suspend fun retrieveKeyFromCloud(): Base58EncodedPrivateKey {
        return Base58EncodedPrivateKey(storage.retrievePrivateKey())
    }

}