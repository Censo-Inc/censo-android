package co.censo.shared.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.JWT_KEY
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.SHARED_PREF_NAME
import javax.inject.Inject

interface SecurePreferences {
    fun saveJWT(jwt: String)
    fun retrieveJWT() : String
    fun clearJWT()
}

class SecurePreferencesImpl @Inject constructor(applicationContext: Context) :
        SecurePreferences {
    object Companion {
        const val SHARED_PREF_NAME = "vault_secure_shared_pref"

        const val JWT_KEY = "jwt"
    }

    private val masterKeyAlias: MasterKey =
        MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private var secureSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        applicationContext,
        SHARED_PREF_NAME,
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveJWT(jwt: String) {
        val editor = secureSharedPreferences.edit()
        editor.putString(JWT_KEY, jwt)
        editor.apply()
    }

    override fun retrieveJWT(): String =
        secureSharedPreferences.getString(JWT_KEY, "") ?: ""


    override fun clearJWT() =
        saveJWT("")


}