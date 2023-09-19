package co.censo.shared.data.repository

import co.censo.shared.data.cryptography.key.KeystoreHelper
import co.censo.shared.data.storage.Storage

interface KeyRepository {
    fun hasKeyWithId(id: String): Boolean
    fun createAndSaveKeyWithId(id: String)
    fun setSavedDeviceId(id: String)
}

class KeyRepositoryImpl(val storage: Storage) : KeyRepository {
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
}