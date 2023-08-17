package co.censo.vault.storage

import android.content.Context
import android.content.SharedPreferences
import co.censo.vault.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue


interface Storage {
    fun saveBIP39Phrases(phrases: BIP39Phrases)
    fun retrieveBIP39Phrases(): BIP39Phrases
    fun clearStoredPhrases()
    fun storedPhrasesIsNotEmpty() : Boolean
}

object SharedPrefsStorage : Storage {

    private const val MAIN_PREFS = "main_prefs"

    private const val BIP39 = "bip_39"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    fun setup(context: Context) {
        appContext = context
        sharedPrefs = appContext.getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
    }

    override fun saveBIP39Phrases(phrases: BIP39Phrases) {
        val editor = sharedPrefs.edit()
        editor.putString(BIP39, jsonMapper.writeValueAsString(phrases))
        editor.apply()
    }

    override fun retrieveBIP39Phrases(): BIP39Phrases {
        return sharedPrefs.getString(BIP39, null)?.let { jsonMapper.readValue<BIP39Phrases>(it) }
            ?: emptyMap()
    }

    override fun clearStoredPhrases() {
        val editor = sharedPrefs.edit()
        editor.putString(BIP39, "")
        editor.apply()
    }

    override fun storedPhrasesIsNotEmpty() = retrieveBIP39Phrases().isNotEmpty()
}