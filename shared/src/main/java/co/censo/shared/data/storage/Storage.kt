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
    fun saveReadHeaders(authHeadersWithTimestamp: AuthInterceptor.AuthHeadersWithTimestamp)
    fun retrieveReadHeaders(): AuthInterceptor.AuthHeadersWithTimestamp?
    fun clearReadHeaders()
}

object SharedPrefsStorage : Storage {

    private const val MAIN_PREFS = "main_prefs"

    private const val USER_SEEN_PERMISSION_DIALOG = "user_seen_permission_dialog"

    private const val DEVICE_CREATED_FLAG = "device_created_flag"

    private const val BIP39 = "bip_39"
    private const val TEMP_ENCRYPTION_MASTER_KEY = "temp_encryption_master_key"

    private const val AUTH_HEADERS = "read_timestamp"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    fun setup(context: Context) {
        appContext = context
        sharedPrefs = appContext.getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
    }

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

    //region read auth headers
    override fun saveReadHeaders(authHeadersWithTimestamp: AuthInterceptor.AuthHeadersWithTimestamp) {
        val editor = sharedPrefs.edit()
        editor.putString(AUTH_HEADERS, Json.encodeToString(authHeadersWithTimestamp))
        editor.apply()
    }

    override fun retrieveReadHeaders(): AuthInterceptor.AuthHeadersWithTimestamp? {
        val authHeadersWithTimestamp = sharedPrefs.getString(AUTH_HEADERS, null)

        if (authHeadersWithTimestamp.isNullOrEmpty()) return null

        return Json.decodeFromString(authHeadersWithTimestamp)
    }

    override fun clearReadHeaders() {
        val editor = sharedPrefs.edit()
        editor.putString(AUTH_HEADERS, "")
        editor.apply()
    }
    //endregion

    override fun storedPhrasesIsNotEmpty() = retrieveBIP39Phrases().isNotEmpty()
}
