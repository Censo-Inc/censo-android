package co.censo.vault.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.AnyThread
import co.censo.vault.AuthHeadersListener
import co.censo.vault.AuthHeadersState
import co.censo.vault.data.networking.AuthInterceptor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface Storage {
    fun saveBIP39Phrases(phrases: BIP39Phrases)
    fun retrieveBIP39Phrases(): BIP39Phrases
    fun clearStoredPhrases()
    fun storedPhrasesIsNotEmpty() : Boolean
    fun saveReadHeaders(authHeadersWithTimestamp: AuthInterceptor.AuthHeadersWithTimestamp)
    fun retrieveReadHeaders(): AuthInterceptor.AuthHeadersWithTimestamp?
    fun clearReadHeaders()

    //Auth Headers Notifying Functionality
    fun setAuthHeadersState(authHeadersState: AuthHeadersState)
    fun addAuthHeadersStateListener(listener: AuthHeadersListener)
    fun clearAllListeners()
}

object SharedPrefsStorage : Storage {

    private const val MAIN_PREFS = "main_prefs"

    private const val BIP39 = "bip_39"

    private const val AUTH_HEADERS = "read_timestamp"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    private val listeners: HashMap<String, AuthHeadersListener> = hashMapOf()

    fun setup(context: Context) {
        appContext = context
        sharedPrefs = appContext.getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
    }

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

    @AnyThread
    override fun setAuthHeadersState(authHeadersState: AuthHeadersState) {
        synchronized(listeners) {
            for (listener in listeners.values) {
                Thread { listener.onAuthHeadersStateChanged(authHeadersState) }.start()
            }
        }
    }

    @AnyThread
    override fun addAuthHeadersStateListener(listener: AuthHeadersListener) {
        synchronized(listeners) {
            listeners.put(listener::class.java.name, listener)
        }
    }

    @AnyThread
    override fun clearAllListeners() {
        synchronized(listeners) { listeners.clear() }
    }
    //endregion

    override fun storedPhrasesIsNotEmpty() = retrieveBIP39Phrases().isNotEmpty()
}
