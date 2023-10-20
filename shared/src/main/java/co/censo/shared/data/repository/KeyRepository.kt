package co.censo.shared.data.repository

import Base58EncodedPrivateKey
import ParticipantId
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.SymmetricEncryption
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.cryptography.sha256digest
import co.censo.shared.data.cryptography.toByteArrayNoSign
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.storage.CloudStorage
import io.github.novacrypto.base58.Base58
import org.bouncycastle.util.encoders.Hex
import co.censo.shared.data.storage.SecurePreferences

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
    fun retrieveSavedDeviceId(): String
    fun retrieveInternalDeviceKey(): InternalDeviceKey
    fun createGuardianKey(): EncryptionKey
    fun encryptWithDeviceKey(data: ByteArray) : ByteArray
    fun decryptWithDeviceKey(data: ByteArray) : ByteArray
    suspend fun saveKeyInCloud(
        key: Base58EncodedPrivateKey,
        participantId: ParticipantId
    )
    suspend fun retrieveKeyFromCloud(participantId: ParticipantId): Base58EncodedPrivateKey
    suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean

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
        participantId: ParticipantId
    ) {
        val encryptedKey = SymmetricEncryption().encrypt(
            retrieveSavedDeviceId().sha256digest(),
            key.bigInt().toByteArrayNoSign()
        )
        //TODO: Save to sharedPrefs for now until Google Drive File Retrieval is in
        storage.savePrivateKey(key = encryptedKey.toHexString())

//        cloudStorage.uploadFile(
//            fileContent = key.value,
//            participantId = participantId,
//        )
    }

    override suspend fun retrieveKeyFromCloud(participantId: ParticipantId): Base58EncodedPrivateKey {
        //TODO: Retrieve from sharedPrefs for now until Google Drive File Retrieval is in
        val encryptedKey = Hex.decode(storage.retrievePrivateKey())
        val decryptedKey = SymmetricEncryption().decrypt(retrieveSavedDeviceId().sha256digest(), encryptedKey)
        return Base58EncodedPrivateKey(Base58.base58Encode(decryptedKey))
    }

    override suspend fun userHasKeySavedInCloud(participantId: ParticipantId): Boolean {
        return retrieveKeyFromCloud(participantId).value.isNotEmpty()
    }

    override suspend fun deleteDeviceKeyIfPresent(keyId: String) {
        KeystoreHelper().deleteDeviceKeyIfPresent(keyId)
    }
}