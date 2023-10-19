package co.censo.shared.util

import android.content.Context
import co.censo.shared.BuildConfig
import co.censo.shared.data.storage.SecurePreferences
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
            onException(e)
        } catch (e: Exception) {
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
                            //TODO: Log with raygun
                            projectLog(message = "Silent Sign In failed")
                        } else {
                            secureStorage.saveJWT(updatedJwt)
                        }

                    }
                },
                onException = {
                    //TODO: Log with raygun
                    projectLog(message = "ApiException: $it")
                }
            )
        }
    }

    override suspend fun signOut() {
        getGoogleSignInClient().signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                projectLog(message = " signOut completed successfully")
            } else {
                projectLog(message = "signOut failed, ${it.exception?.message}")
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

