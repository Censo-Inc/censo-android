package co.censo.shared.data.storage

import android.content.Context
import android.content.SharedPreferences
import co.censo.shared.BuildConfig
import co.censo.shared.data.model.SecurityPlanData
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
    fun saveAuthAccessToken(token: String)
    fun retrieveAuthAccessToken() : String
    fun clearAuthAccessToken()
    fun saveGuardianInvitationId(id: String)
    fun retrieveGuardianInvitationId() : String
    fun clearGuardianInvitationId()
    fun saveGuardianParticipantId(id: String)
    fun retrieveGuardianParticipantId() : String
    fun clearGuardianParticipantId()
    fun savePrivateKey(key: String)
    fun retrievePrivateKey() : String
    fun clearPrivateKey()
    fun setEditingSecurityPlan(editingSecurityPlan: Boolean)
    fun isEditingSecurityPlan() : Boolean
    fun setSecurityPlan(securityPlanData: SecurityPlanData)
    fun clearSecurityPlanData()
    fun retrieveSecurityPlan() : SecurityPlanData?
}

object SharedPrefsStorage : Storage {

    private const val MAIN_PREFS = "main_prefs"

    private const val USER_SEEN_PERMISSION_DIALOG = "user_seen_permission_dialog"

    private const val DEVICE_CREATED_FLAG = "device_created_flag"

    private const val BIP39 = "bip_39"

    private const val DEVICE_KEY = "device_key"

    private const val GUARDIAN_INVITATION_ID = "guardian_invitation_id"

    private const val GUARDIAN_PARTICIPANT_ID = "guardian_participant_id"

    private const val JWT = "jwt"
    private const val GOOGLE_AUTH_ACCESS_TOKEN = "google_auth_access_token"

    private const val GOOGLE_DRIVE_MOCK_KEY = "google_drive_mock_key"

    private const val EDITING_SECURITY_PLAN = "editing_security_plan"

    private const val SECURITY_PLAN = "security_plan"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    fun setup(context: Context) {
        appContext = context
        sharedPrefs = appContext.getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
    }

    //region editing security plan
    override fun isEditingSecurityPlan(): Boolean {
        return sharedPrefs.getBoolean(EDITING_SECURITY_PLAN, false)
    }

    override fun setEditingSecurityPlan(editingSecurityPlan: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(EDITING_SECURITY_PLAN, editingSecurityPlan)
        editor.apply()
    }
    //endregion

    //region security plan
    override fun setSecurityPlan(securityPlanData: SecurityPlanData) {
        val editor = sharedPrefs.edit()
        editor.putString(SECURITY_PLAN, Json.encodeToString(securityPlanData))
        editor.apply()
    }

    override fun clearSecurityPlanData() {
        val editor = sharedPrefs.edit()
        editor.putString(SECURITY_PLAN, "")
        editor.apply()
    }

    override fun retrieveSecurityPlan(): SecurityPlanData? {
        val securityJson = sharedPrefs.getString(SECURITY_PLAN, "") ?: ""

        if (securityJson.isEmpty()) return null

        return Json.decodeFromString(securityJson)
    }
    //endregion

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

    //region JWT + AccessToken
    override fun saveJWT(jwt: String) {
        val editor = sharedPrefs.edit()
        editor.putString(JWT, jwt)
        editor.apply()
    }

    override fun retrieveJWT() =
        sharedPrefs.getString(JWT, "") ?: ""

    override fun clearJWT() = saveJWT("")
    override fun saveAuthAccessToken(token: String) {
        val editor = sharedPrefs.edit()
        editor.putString(GOOGLE_AUTH_ACCESS_TOKEN, token)
        editor.apply()
    }

    override fun retrieveAuthAccessToken(): String =
        sharedPrefs.getString(GOOGLE_AUTH_ACCESS_TOKEN, "") ?: ""


    override fun clearAuthAccessToken() = saveAuthAccessToken("")
    //endregion

    //region Guardian Invitation Id
    override fun saveGuardianInvitationId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(GUARDIAN_INVITATION_ID, id)
        editor.apply()
    }

    override fun retrieveGuardianInvitationId() =
        sharedPrefs.getString(GUARDIAN_INVITATION_ID, "") ?: ""

    override fun clearGuardianInvitationId() = saveGuardianInvitationId("")
    //endregion

    //region Guardian Participant Id
    override fun saveGuardianParticipantId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(GUARDIAN_PARTICIPANT_ID, id)
        editor.apply()
    }

    override fun retrieveGuardianParticipantId() =
        sharedPrefs.getString(GUARDIAN_PARTICIPANT_ID, "") ?: ""

    override fun clearGuardianParticipantId() = saveGuardianParticipantId("")
    //endregion

    //region Mock Google Drive Private Key
    override fun savePrivateKey(key: String) {
        if(BuildConfig.BUILD_TYPE != "debug") {
            throw Exception("This is a temporary mocking of google drive only to be used on debug builds")
        }

        val editor = sharedPrefs.edit()
        editor.putString(GOOGLE_DRIVE_MOCK_KEY, key)
        editor.apply()
    }

    override fun retrievePrivateKey() : String {
        if(BuildConfig.BUILD_TYPE != "debug") {
            throw Exception("This is a temporary mocking of google drive only to be used on debug builds")
        }

        return sharedPrefs.getString(GOOGLE_DRIVE_MOCK_KEY, "") ?: ""
    }

    override fun clearPrivateKey() = savePrivateKey("")
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
