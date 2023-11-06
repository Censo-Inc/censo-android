package co.censo.shared.util

import android.content.Context
import co.censo.shared.BuildConfig
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.CrashReportingUtil.RetrieveAccount
import co.censo.shared.util.CrashReportingUtil.SilentRefreshToken
import com.auth0.android.jwt.JWT
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock

interface AuthUtil {
    suspend fun silentlyRefreshTokenIfInvalid(jwt: String, deviceKeyId: String)
    fun getGoogleSignInClient() : GoogleSignInClient
    fun isJWTValid(jwt: JWT): Boolean
    fun getAccountFromSignInTask(
        task: Task<GoogleSignInAccount>,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onException: (Exception) -> Unit
    )
    fun verifyToken(jwt: String) : String?

    suspend fun signOut()
}

class GoogleAuth(private val context: Context, private val secureStorage: SecurePreferences) : AuthUtil {
    companion object {
        val DRIVE_FILE_SCOPE = Scope(DriveScopes.DRIVE_FILE)
    }

    private fun createGoogleSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder()
            .requestIdToken(BuildConfig.GOOGLE_AUTH_SERVER_ID)
            .requestEmail()
            .requestScopes(DRIVE_FILE_SCOPE)
            .build()


    override fun getAccountFromSignInTask(
        task: Task<GoogleSignInAccount>,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onException: (Exception) -> Unit
    ) {
        try {
            val account = task.getResult(ApiException::class.java)
            onSuccess(account)
        } catch (e: ApiException) {
            e.sendError(RetrieveAccount)
            onException(e)
        } catch (e: Exception) {
            e.sendError(RetrieveAccount)
            onException(e)
        }
    }

    override suspend fun silentlyRefreshTokenIfInvalid(jwt: String, deviceKeyId: String) {
        val decodedJwt = JWT(jwt)
        if (!isJWTValid(decodedJwt)) {
            val googleSignInClient = getGoogleSignInClient()

            getAccountFromSignInTask(
                task = googleSignInClient.silentSignIn(),
                onSuccess = { account ->
                    val updatedJwt = account.idToken
                    if (updatedJwt?.isNotEmpty() == true) {
                        val verifiedIdToken = verifyToken(updatedJwt)
                        if (verifiedIdToken == null || verifiedIdToken != deviceKeyId) {
                            Exception().sendError(SilentRefreshToken)
                        } else {
                            secureStorage.saveJWT(updatedJwt)
                        }

                    }
                },
                onException = {
                    it.sendError(SilentRefreshToken)
                }
            )
        }
    }

    override suspend fun signOut() {
        getGoogleSignInClient().signOut().addOnCompleteListener {
            if (it.isSuccessful) {
            } else {
                it.exception?.sendError(CrashReportingUtil.SignOut)
            }
        }.await()
    }

    override fun getGoogleSignInClient(): GoogleSignInClient = GoogleSignIn.getClient(context, createGoogleSignInOptions())

    override fun isJWTValid(jwt: JWT) : Boolean {
        val expirationTime = jwt.expiresAt?.toInstant() ?: return false
        val currentTime = Clock.System.now()

        return currentTime.epochSeconds  < expirationTime.epochSecond
    }

    override fun verifyToken(token: String): String? {
        val verifier = GoogleIdTokenVerifier.Builder(
            NetHttpTransport(), GsonFactory()
        )
            .setAudience(BuildConfig.GOOGLE_AUTH_CLIENT_IDS.toList())
            .build()

        val verifiedIdToken: GoogleIdToken? = verifier.verify(token)
        return verifiedIdToken?.payload?.subject
    }
}

