package co.censo.shared.data.storage

import android.content.Context
import android.content.SharedPreferences
import co.censo.shared.data.networking.AuthInterceptor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface Storage {
    fun setDeviceCreatedFlag()
    fun retrieveDeviceCreatedFlag() : Boolean
    fun saveBIP39Phrases(phrases: BIP39Phrases)
    fun retrieveBIP39Phrases(): BIP39Phrases
    fun clearStoredPhrases()
    fun storedPhrasesIsNotEmpty() : Boolean
    fun saveDeviceKeyId(id: String)
    fun retrieveDeviceKeyId() : String
    fun clearDeviceKeyId()
    fun saveJWT(jwt: String)
    fun retrieveJWT() : String
    fun clearJWT()
}

object SharedPrefsStorage : Storage {

    private const val MAIN_PREFS = "main_prefs"

    private const val USER_SEEN_PERMISSION_DIALOG = "user_seen_permission_dialog"

    private const val DEVICE_CREATED_FLAG = "device_created_flag"

    private const val BIP39 = "bip_39"

    private const val DEVICE_KEY = "device_key"

    private const val JWT = "jwt"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    fun setup(context: Context) {
        appContext = context
        sharedPrefs = appContext.getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
    }

    //region device key id
    override fun saveDeviceKeyId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(DEVICE_KEY, id)
        editor.apply()
    }

    override fun retrieveDeviceKeyId() =
        sharedPrefs.getString(DEVICE_KEY, "") ?: ""

    override fun clearDeviceKeyId() = saveDeviceKeyId("")
    //endregion

    //region JWT
    override fun saveJWT(jwt: String) {
        val editor = sharedPrefs.edit()
        editor.putString(JWT, jwt)
        editor.apply()
    }

    override fun retrieveJWT() =
        sharedPrefs.getString(JWT, "") ?: ""

    override fun clearJWT() = saveJWT("")
    //endregion

    //region push dialog
    fun userHasSeenPermissionDialog(): Boolean {
        return sharedPrefs.getBoolean(USER_SEEN_PERMISSION_DIALOG, false)
    }

    fun setUserSeenPermissionDialog(seenDialog: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(USER_SEEN_PERMISSION_DIALOG, seenDialog)
        editor.apply()
    }
    //endregion

    //region device created
    override fun setDeviceCreatedFlag() {
        val editor = sharedPrefs.edit()
        editor.putBoolean(DEVICE_CREATED_FLAG, true)
        editor.apply()
    }

    override fun retrieveDeviceCreatedFlag(): Boolean {
        return sharedPrefs.getBoolean(DEVICE_CREATED_FLAG, false)
    }
    //endregion

    //region bip 39 phrases
    override fun saveBIP39Phrases(phrases: BIP39Phrases) {
        val editor = sharedPrefs.edit()
        editor.putString(BIP39, Json.encodeToString(phrases))
        editor.apply()
    }

    override fun retrieveBIP39Phrases(): BIP39Phrases {
        val savedPhraseJson = sharedPrefs.getString(BIP39, null)

        if (savedPhraseJson.isNullOrEmpty()) return emptyMap()

        return Json.decodeFromString(savedPhraseJson)
    }

    override fun clearStoredPhrases() {
        val editor = sharedPrefs.edit()
        editor.putString(BIP39, "")
        editor.apply()
    }
    //endregion

    override fun storedPhrasesIsNotEmpty() = retrieveBIP39Phrases().isNotEmpty()
}
