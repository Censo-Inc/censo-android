package co.censo.shared.data.networking

import InitBiometryVerificationApiResponse
import VaultSecretId
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import co.censo.shared.BuildConfig
import co.censo.shared.data.Header
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ApproveRecoveryApiRequest
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.ConfirmGuardianshipApiRequest
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiRequest
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.InitiateRecoveryApiRequest
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.RejectGuardianVerificationApiResponse
import co.censo.shared.data.model.RejectRecoveryApiResponse
import co.censo.shared.data.model.ReplacePolicyApiRequest
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveRecoveryShardsApiRequest
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiRequest
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiResponse
import co.censo.shared.data.model.StoreSecretApiRequest
import co.censo.shared.data.model.StoreSecretApiResponse
import co.censo.shared.data.model.SubmitGuardianVerificationApiRequest
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.networking.ApiService.Companion.APPLICATION_IDENTIFIER
import co.censo.shared.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.shared.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.shared.data.networking.ApiService.Companion.IS_API
import co.censo.shared.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.projectLog
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.IOException
import java.time.Duration
import java.util.Base64
import retrofit2.Response as RetrofitResponse

interface ApiService {

    companion object {

        const val INVITATION_ID = "invitationId"
        const val INTERMEDIATE_KEY = "intermediateKey"
        const val PARTICIPANT_ID = "participantId"
        const val SECRET_ID = "secretId"

        const val IS_API = "X-IsApi"
        const val APPLICATION_IDENTIFIER = "X-Censo-App-Identifier"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"

        fun create(
            secureStorage: SecurePreferences,
            context: Context,
            versionCode: String,
            packageName: String,
            authUtil: AuthUtil
        ): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(ConnectivityInterceptor(context))
                .addInterceptor(AnalyticsInterceptor(versionCode, packageName))
                .addInterceptor(AuthInterceptor(authUtil, secureStorage))
                .connectTimeout(Duration.ofSeconds(180))
                .readTimeout(Duration.ofSeconds(180))
                .callTimeout(Duration.ofSeconds(180))
                .writeTimeout(Duration.ofSeconds(180))


            if (BuildConfig.BUILD_TYPE == "debug") {
                val logger =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                client.addInterceptor(logger)
            }

            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client.build())
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }.asConverterFactory(
                        contentType
                    )
                )
                .build()
                .create(ApiService::class.java)
        }
    }


    @POST("/v1/sign-in")
    suspend fun signIn(@Body signInApiRequest: SignInApiRequest): RetrofitResponse<ResponseBody>

    @GET("/v1/user")
    suspend fun user(): RetrofitResponse<GetUserApiResponse>

    @DELETE("/v1/user")
    suspend fun deleteUser(): RetrofitResponse<Unit>

    @POST("/v1/biometry-verifications")
    suspend fun initBiometryVerification(): RetrofitResponse<InitBiometryVerificationApiResponse>

    @POST("/v1/policy-setup")
    suspend fun createOrUpdatePolicySetup(
        @Body apiRequest: CreatePolicySetupApiRequest
    ): RetrofitResponse<CreatePolicySetupApiResponse>

    @POST("/v1/policy")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest
    ): RetrofitResponse<CreatePolicyApiResponse>

    @PUT("/v1/policy")
    suspend fun replacePolicy(
        @Body createPolicyApiRequest: ReplacePolicyApiRequest
    ): RetrofitResponse<ReplacePolicyApiResponse>

    @POST("/v1/guardianship-invitations/{$INVITATION_ID}/accept")
    suspend fun acceptGuardianship(
        @Path(value = INVITATION_ID) invitationId: String
    ): RetrofitResponse<AcceptGuardianshipApiResponse>

    @POST("/v1/guardianship-invitations/{$INVITATION_ID}/decline")
    suspend fun declineGuardianship(
        @Path(value = INVITATION_ID) invitationId: String,
    ): RetrofitResponse<ResponseBody>

    @POST("v1/guardianship-invitations/{$INVITATION_ID}/verification")
    suspend fun submitGuardianVerification(
        @Path(value = INVITATION_ID) invitationId: String,
        @Body submitGuardianVerificationApiRequest: SubmitGuardianVerificationApiRequest
    ): RetrofitResponse<SubmitGuardianVerificationApiResponse>

    @POST("/v1/guardians/{$PARTICIPANT_ID}/confirmation")
    suspend fun confirmGuardianship(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body confirmGuardianshipApiRequest: ConfirmGuardianshipApiRequest
    ): RetrofitResponse<ConfirmGuardianshipApiResponse>

    @POST("/v1/guardians/{$PARTICIPANT_ID}/verification/reject")
    suspend fun rejectVerification(
        @Path(value = PARTICIPANT_ID) participantId: String,
    ): RetrofitResponse<RejectGuardianVerificationApiResponse>

    @POST("v1/notification-tokens")
    suspend fun addPushNotificationToken(@Body pushData: PushBody): RetrofitResponse<Unit>

    @DELETE("v1/notification-tokens/{deviceType}")
    suspend fun removePushNotificationToken(
        @Path("deviceType") deviceType: String
    ): RetrofitResponse<Unit>

    @POST("/v1/unlock")
    suspend fun unlock(
        @Body apiRequest: UnlockApiRequest
    ): RetrofitResponse<UnlockApiResponse>

    @POST("/v1/unlock-prolongation")
    suspend fun prolongUnlock(): RetrofitResponse<ProlongUnlockApiResponse>

    @POST("/v1/lock")
    suspend fun lock(): RetrofitResponse<LockApiResponse>

    @POST("/v1/vault/secrets")
    suspend fun storeSecret(
        @Body apiRequest: StoreSecretApiRequest
    ): RetrofitResponse<StoreSecretApiResponse>

    @DELETE("/v1/vault/secrets/{$SECRET_ID}")
    suspend fun deleteSecret(
        @Path(value = SECRET_ID) secretId: VaultSecretId
    ): RetrofitResponse<DeleteSecretApiResponse>

    @POST("/v1/recovery")
    suspend fun requestRecovery(): RetrofitResponse<InitiateRecoveryApiResponse>

    @DELETE("/v1/recovery")
    suspend fun deleteRecovery(): RetrofitResponse<DeleteRecoveryApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/totp")
    suspend fun storeRecoveryTotpSecret(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: StoreRecoveryTotpSecretApiRequest
    ): RetrofitResponse<StoreRecoveryTotpSecretApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/totp-verification")
    suspend fun submitRecoveryTotpVerification(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: SubmitRecoveryTotpVerificationApiRequest
    ): RetrofitResponse<SubmitRecoveryTotpVerificationApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/approval")
    suspend fun approveRecovery(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: ApproveRecoveryApiRequest
    ): RetrofitResponse<ApproveRecoveryApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/rejection")
    suspend fun rejectRecovery(
        @Path(value = PARTICIPANT_ID) participantId: String
    ): RetrofitResponse<RejectRecoveryApiResponse>

    @POST("/v1/recovery/retrieval")
    suspend fun retrieveRecoveryShards(
        @Body apiRequest: RetrieveRecoveryShardsApiRequest
    ): RetrofitResponse<RetrieveRecoveryShardsApiResponse>
}

class AnalyticsInterceptor(
    private val versionCode: String,
    private val packageName: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) =
        chain.proceed(
            chain.request().newBuilder()
                .apply {
                    addHeader(
                        IS_API,
                        "true"
                    )
                    addHeader(
                        APPLICATION_IDENTIFIER,
                        packageName
                    )
                    addHeader(
                        DEVICE_TYPE_HEADER,
                        "Android ${Build.MANUFACTURER} - ${Build.DEVICE} (${Build.MODEL})"
                    )
                    addHeader(
                        APP_VERSION_HEADER,
                        "${BuildConfig.VERSION_NAME} ($versionCode)"
                    )
                    addHeader(
                        OS_VERSION_HEADER,
                        "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"
                    )
                }
                .build()
        )
}

class AuthInterceptor(
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences
) : Interceptor {
    private val AUTHORIZATION_HEADER = "Authorization"
    private val DEVICE_PUBLIC_KEY_HEADER = "X-Censo-Device-Public-Key"
    private val TIMESTAMP_HEADER = "X-Censo-Timestamp"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val now = Clock.System.now()
        var headers : List<Header>

        runBlocking {
            headers = getAuthHeaders(now, request)
        }

        return if (headers.isEmpty()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder().apply {
                    for (header in headers) {
                        addHeader(header.name, header.value)
                    }
                }.build()
            )
        }
    }

    private suspend fun getAuthHeaders(now: Instant, request: Request): List<Header> {
        val deviceKeyId = secureStorage.retrieveDeviceKeyId()
        val jwt = secureStorage.retrieveJWT()

        if (jwt.isEmpty() || deviceKeyId.isEmpty()) {
            return emptyList()
        }

        try {
            authUtil.silentlyRefreshTokenIfInvalid(jwt, deviceKeyId)
        } catch (e: Exception) {
            //TODO: Log with raygun
            projectLog(message = "Exception thrown: $e")
            return emptyList()
        }

        val deviceKey = InternalDeviceKey(deviceKeyId)
        val signature = Base64.getEncoder().encodeToString(deviceKey.sign(dataToSign(request, now)))

        return listOf(
            Header(AUTHORIZATION_HEADER, "signature $signature"),
            Header(DEVICE_PUBLIC_KEY_HEADER, deviceKey.publicExternalRepresentation().value),
            Header(TIMESTAMP_HEADER, now.toString())
        )
    }

    private fun dataToSign(request: Request, timestamp: Instant): ByteArray {
        val requestPathAndQueryParams =
            request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: "")
        val requestBody = request.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            buffer.readByteArray()
        } ?: byteArrayOf()

        return (request.method + requestPathAndQueryParams + Base64.getEncoder()
            .encodeToString(requestBody) + timestamp.toString()).toByteArray()
    }
}

class ConnectivityInterceptor(private val context: Context) : Interceptor {

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkInfo != null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isOnline(context = context)) {
            throw NoConnectivityException()
        } else {
            return chain.proceed(chain.request())
        }
    }
}

class NoConnectivityException : IOException() {
    override val message: String
        get() = "No network connection established"
}

@Serializable
data class PushBody(
    val deviceType: String, val token: String
)
