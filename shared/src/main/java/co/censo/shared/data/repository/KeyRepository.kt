package co.censo.shared.data.repository

import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.storage.Storage
import java.security.PrivateKey

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
    fun retrieveInternalDeviceKey() : InternalDeviceKey
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
}