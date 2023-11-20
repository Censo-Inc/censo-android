package co.censo.shared.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.censo.shared.BuildConfig
import co.censo.shared.data.model.SecurityPlanData
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.ACCEPTED_TERMS_OF_USE_VERSION
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.APPROVER_APPROVAL_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.BIP39
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.DEVICE_CREATED_FLAG
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.EDITING_SECURITY_PLAN
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.GUARDIAN_INVITATION_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.GUARDIAN_PARTICIPANT_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.JWT_KEY
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.SHARED_PREF_NAME
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.SECURITY_PLAN
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.USER_SEEN_PERMISSION_DIALOG
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.DEVICE_KEY
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface SecurePreferences {
    fun saveJWT(jwt: String)
    fun retrieveJWT() : String
    fun clearJWT()
    fun setDeviceCreatedFlag()
    fun retrieveDeviceCreatedFlag() : Boolean
    fun saveBIP39Phrases(phrases: BIP39Phrases)
    fun retrieveBIP39Phrases(): BIP39Phrases
    fun clearStoredPhrases()
    fun storedPhrasesIsNotEmpty() : Boolean
    fun saveDeviceKeyId(id: String)
    fun retrieveDeviceKeyId() : String
    fun clearDeviceKeyId()
    fun saveGuardianInvitationId(id: String)
    fun retrieveGuardianInvitationId() : String
    fun clearGuardianInvitationId()
    fun saveGuardianParticipantId(id: String)
    fun retrieveGuardianParticipantId() : String
    fun clearGuardianParticipantId()
    fun setEditingSecurityPlan(editingSecurityPlan: Boolean)
    fun isEditingSecurityPlan() : Boolean
    fun setSecurityPlan(securityPlanData: SecurityPlanData)
    fun clearSecurityPlanData()
    fun retrieveSecurityPlan() : SecurityPlanData?
    fun userHasSeenPermissionDialog(): Boolean
    fun setUserSeenPermissionDialog(seenDialog: Boolean)
    fun acceptedTermsOfUseVersion(): String
    fun setAcceptedTermsOfUseVersion(version: String)
    fun saveApprovalId(id: String)
    fun retrieveApprovalId() : String
    fun clearApprovalId()
}

class SecurePreferencesImpl @Inject constructor(applicationContext: Context) :
        SecurePreferences {
    object Companion {
        const val SHARED_PREF_NAME = "vault_secure_shared_pref"

        //Stored Values
        const val JWT_KEY = "jwt"
        const val USER_SEEN_PERMISSION_DIALOG = "user_seen_permission_dialog"
        const val DEVICE_CREATED_FLAG = "device_created_flag"
        const val BIP39 = "bip_39"
        const val DEVICE_KEY = "device_key"
        const val GUARDIAN_INVITATION_ID = "guardian_invitation_id"
        const val GUARDIAN_PARTICIPANT_ID = "guardian_participant_id"
        const val APPROVER_APPROVAL_ID = "approver_approval_id"
        const val EDITING_SECURITY_PLAN = "editing_security_plan"
        const val SECURITY_PLAN = "security_plan"
        const val ACCEPTED_TERMS_OF_USE_VERSION = "accepted_terms_of_use_version"
    }

    private val masterKeyAlias: MasterKey =
        MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private var sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        applicationContext,
        SHARED_PREF_NAME,
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveJWT(jwt: String) {
        val editor = sharedPrefs.edit()
        editor.putString(JWT_KEY, jwt)
        editor.apply()
    }

    override fun retrieveJWT(): String =
        sharedPrefs.getString(JWT_KEY, "") ?: ""


    override fun clearJWT() =
        saveJWT("")

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

    //region Approval Id
    override fun saveApprovalId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(APPROVER_APPROVAL_ID, id)
        editor.apply()
    }

    override fun retrieveApprovalId() =
        sharedPrefs.getString(APPROVER_APPROVAL_ID, "") ?: ""

    override fun clearApprovalId() = saveApprovalId("")
    //endregion

    //region push dialog
    override fun userHasSeenPermissionDialog(): Boolean {
        return sharedPrefs.getBoolean(USER_SEEN_PERMISSION_DIALOG, false)
    }

    override fun setUserSeenPermissionDialog(seenDialog: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(USER_SEEN_PERMISSION_DIALOG, seenDialog)
        editor.apply()
    }
    //endregion

    //region terms of use
    override fun acceptedTermsOfUseVersion(): String {
        return sharedPrefs.getString(ACCEPTED_TERMS_OF_USE_VERSION, "") ?: ""
    }

    override fun setAcceptedTermsOfUseVersion(version: String) {
        val editor = sharedPrefs.edit()
        editor.putString(ACCEPTED_TERMS_OF_USE_VERSION, version)
        editor.apply()
    }
    // endregion

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