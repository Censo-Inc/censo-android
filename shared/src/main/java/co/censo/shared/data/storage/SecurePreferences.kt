package co.censo.shared.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.ACCEPTED_TERMS_OF_USE_VERSION
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.APPROVER_APPROVAL_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.APPROVER_INVITATION_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.APPROVER_PARTICIPANT_ID
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.JWT_KEY
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.SHARED_PREF_NAME
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.USER_SEEN_PERMISSION_DIALOG
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.DEVICE_KEY
import co.censo.shared.data.storage.SecurePreferencesImpl.Companion.LOGIN_ID_RESET_TOKENS
import javax.inject.Inject

interface SecurePreferences {
    fun saveJWT(jwt: String)
    fun retrieveJWT() : String
    fun clearJWT()
    fun saveDeviceKeyId(id: String)
    fun retrieveDeviceKeyId() : String
    fun clearDeviceKeyId()
    fun saveApproverInvitationId(id: String)
    fun retrieveApproverInvitationId() : String
    fun clearApproverInvitationId()
    fun saveApproverParticipantId(id: String)
    fun retrieveApproverParticipantId() : String
    fun clearApproverParticipantId()
    fun userHasSeenPermissionDialog(): Boolean
    fun setUserSeenPermissionDialog(seenDialog: Boolean)
    fun acceptedTermsOfUseVersion(): String
    fun setAcceptedTermsOfUseVersion(version: String)
    fun saveApprovalId(id: String)
    fun retrieveApprovalId() : String
    fun clearApprovalId()
    fun saveResetTokens(resetTokens: Set<String>)
    fun retrieveResetTokens(): Set<String>
    fun clearResetTokens()
}

class SecurePreferencesImpl @Inject constructor(applicationContext: Context) :
        SecurePreferences {
    object Companion {
        const val SHARED_PREF_NAME = "vault_secure_shared_pref"

        //Stored Values
        const val JWT_KEY = "jwt"
        const val USER_SEEN_PERMISSION_DIALOG = "user_seen_permission_dialog"
        const val DEVICE_KEY = "device_key"
        const val APPROVER_INVITATION_ID = "approver_invitation_id"
        const val APPROVER_PARTICIPANT_ID = "approver_participant_id"
        const val LOGIN_ID_RESET_TOKENS = "login_id_reset_tokens"
        const val APPROVER_APPROVAL_ID = "approver_approval_id"
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

    //region Approver Invitation Id
    override fun saveApproverInvitationId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(APPROVER_INVITATION_ID, id)
        editor.apply()
    }

    override fun retrieveApproverInvitationId() =
        sharedPrefs.getString(APPROVER_INVITATION_ID, "") ?: ""

    override fun clearApproverInvitationId() = saveApproverInvitationId("")
    //endregion

    //region Approver Participant Id
    override fun saveApproverParticipantId(id: String) {
        val editor = sharedPrefs.edit()
        editor.putString(APPROVER_PARTICIPANT_ID, id)
        editor.apply()
    }

    override fun retrieveApproverParticipantId() =
        sharedPrefs.getString(APPROVER_PARTICIPANT_ID, "") ?: ""

    override fun clearApproverParticipantId() = saveApproverParticipantId("")
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

    //region Reset Tokens
    override fun saveResetTokens(resetTokens: Set<String>) {
        val editor = sharedPrefs.edit()
        editor.putStringSet(LOGIN_ID_RESET_TOKENS, resetTokens)
        editor.apply()
    }

    override fun retrieveResetTokens(): Set<String> {
        return sharedPrefs.getStringSet(LOGIN_ID_RESET_TOKENS, null) ?: setOf()
    }

    override fun clearResetTokens() = saveResetTokens(setOf())
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
}