package co.censo.shared.data.networking

import InitBiometryVerificationApiResponse
import VaultSecretId
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import co.censo.shared.BuildConfig
import co.censo.shared.data.Header
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.sha256digest
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ApproveRecoveryApiRequest
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.AttestationChallengeResponse
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiRequest
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiResponse
import co.censo.shared.data.model.ConfirmGuardianshipApiRequest
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiRequest
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
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
import co.censo.shared.data.model.SubmitPurchaseApiRequest
import co.censo.shared.data.model.SubmitPurchaseApiResponse
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.networking.ApiService.Companion.APPLICATION_IDENTIFIER
import co.censo.shared.data.networking.ApiService.Companion.APP_PLATFORM_HEADER
import co.censo.shared.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.shared.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.shared.data.networking.ApiService.Companion.IS_API
import co.censo.shared.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.shared.data.repository.PlayIntegrityRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
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
import retrofit2.http.HeaderMap
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
        const val APPROVAL_ID = "approvalId"
        const val SECRET_ID = "secretId"

        const val IS_API = "X-IsApi"
        const val APPLICATION_IDENTIFIER = "X-Censo-App-Identifier"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val APP_PLATFORM_HEADER = "X-Censo-App-Platform"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"
        const val PLAY_INTEGRITY_HEADER = "X-Censo-Play-Integrity"
        const val PLAY_INTEGRITY_TOKEN_HEADER = "X-Censo-Play-Integrity-Token"
        const val ATTESTATION_CHALLENGE_HEADER = "X-Censo-Challenge"

        fun create(
            secureStorage: SecurePreferences,
            context: Context,
            versionCode: String,
            packageName: String,
            authUtil: AuthUtil,
            playIntegrityRepository: PlayIntegrityRepository
        ): ApiService {

            val playIntegrityInterceptor = PlayIntegrityInterceptor(playIntegrityRepository)
            val client = OkHttpClient.Builder()
                .addInterceptor(ConnectivityInterceptor(context))
                .addInterceptor(AnalyticsInterceptor(versionCode, packageName))
                .addInterceptor(AuthInterceptor(authUtil, secureStorage))
                .addInterceptor(playIntegrityInterceptor)
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
            val apiService = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client.build())
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }.asConverterFactory(
                        contentType
                    )
                )
                .build()
                .create(ApiService::class.java)
            playIntegrityInterceptor.apiService = apiService
            return apiService
        }

        val enablePlayIntegrity = mapOf(PLAY_INTEGRITY_HEADER to "true")
    }


    @POST("/v1/sign-in")
    suspend fun signIn(
        @Body signInApiRequest: SignInApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ResponseBody>

    @GET("/v1/user")
    suspend fun ownerUser(): RetrofitResponse<GetOwnerUserApiResponse>

    @GET("/v1/user")
    suspend fun approverUser(): RetrofitResponse<GetApproverUserApiResponse>

    @DELETE("/v1/user")
    suspend fun deleteUser(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<Unit>

    @POST("/v1/biometry-verifications")
    suspend fun initBiometryVerification(): RetrofitResponse<InitBiometryVerificationApiResponse>

    @POST("/v1/policy-setup")
    suspend fun createOrUpdatePolicySetup(
        @Body apiRequest: CreatePolicySetupApiRequest
    ): RetrofitResponse<CreatePolicySetupApiResponse>

    @POST("/v1/policy")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<CreatePolicyApiResponse>

    @PUT("/v1/policy")
    suspend fun replacePolicy(
        @Body createPolicyApiRequest: ReplacePolicyApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
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
        @Body submitGuardianVerificationApiRequest: SubmitGuardianVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitGuardianVerificationApiResponse>

    @POST("/v1/guardians/{$PARTICIPANT_ID}/confirmation")
    suspend fun confirmGuardianship(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body confirmGuardianshipApiRequest: ConfirmGuardianshipApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
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
        @Path(value = SECRET_ID) secretId: VaultSecretId,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<DeleteSecretApiResponse>

    @POST("/v1/recovery")
    suspend fun requestRecovery(
        @Body apiRequest: InitiateRecoveryApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<InitiateRecoveryApiResponse>

    @DELETE("/v1/recovery")
    suspend fun deleteRecovery(): RetrofitResponse<DeleteRecoveryApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/totp")
    suspend fun storeRecoveryTotpSecret(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: StoreRecoveryTotpSecretApiRequest
    ): RetrofitResponse<StoreRecoveryTotpSecretApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/totp")
    suspend fun storeAccessTotpSecret(
        @Path(value = APPROVAL_ID) approvalId: String,
        @Body apiRequest: StoreRecoveryTotpSecretApiRequest
    ): RetrofitResponse<StoreRecoveryTotpSecretApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/totp-verification")
    suspend fun submitRecoveryTotpVerification(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: SubmitRecoveryTotpVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitRecoveryTotpVerificationApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/approval")
    suspend fun approveRecovery(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: ApproveRecoveryApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ApproveRecoveryApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/approval")
    suspend fun approveAccess(
        @Path(value = APPROVAL_ID) approvalId: String,
        @Body apiRequest: ApproveRecoveryApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ApproveRecoveryApiResponse>

    @POST("/v1/recovery/{$PARTICIPANT_ID}/rejection")
    suspend fun rejectRecovery(
        @Path(value = PARTICIPANT_ID) participantId: String
    ): RetrofitResponse<RejectRecoveryApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/rejection")
    suspend fun rejectAccess(
        @Path(value = APPROVAL_ID) approvalId: String
    ): RetrofitResponse<RejectRecoveryApiResponse>

    @POST("/v1/recovery/retrieval")
    suspend fun retrieveRecoveryShards(
        @Body apiRequest: RetrieveRecoveryShardsApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<RetrieveRecoveryShardsApiResponse>

    @POST("/v1/purchases")
    suspend fun submitPurchase(
        @Body apiRequest: SubmitPurchaseApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitPurchaseApiResponse>


    @POST("v1/guardians/{$PARTICIPANT_ID}/owner-completion")
    suspend fun completeOwnerGuardianship(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: CompleteOwnerGuardianshipApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<CompleteOwnerGuardianshipApiResponse>

    @POST("/v1/attestation-challenge")
    suspend fun createAttestationChallenge(): RetrofitResponse<AttestationChallengeResponse>
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
                        "${BuildConfig.VERSION_NAME}+$versionCode"
                    )
                    addHeader(
                        APP_PLATFORM_HEADER,
                        "android"
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
            e.sendError(CrashReportingUtil.AuthHeaders)
            return emptyList()
        }

        val deviceKey = InternalDeviceKey(deviceKeyId)
        val signature = Base64.getEncoder().encodeToString(deviceKey.sign(request.dataToSign(now.toString())))

        return listOf(
            Header(AUTHORIZATION_HEADER, "signature $signature"),
            Header(DEVICE_PUBLIC_KEY_HEADER, deviceKey.publicExternalRepresentation().value),
            Header(TIMESTAMP_HEADER, now.toString())
        )
    }
}

class PlayIntegrityInterceptor(private val playIntegrityRepository: PlayIntegrityRepository) : Interceptor {

    lateinit var apiService: ApiService

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var headers : List<Header>

        return when {
            !BuildConfig.PLAY_INTEGRITY_ENABLED -> {
                chain.proceed(request.newBuilder().apply {
                    removeHeader(ApiService.PLAY_INTEGRITY_HEADER)
                }.build())
            }

            request.header(ApiService.PLAY_INTEGRITY_HEADER) == null ->
                chain.proceed(request)

            else -> {
                runBlocking {
                    headers = getPlayIntegrityHeaders(request)
                }
                chain.proceed(
                    request.newBuilder().apply {
                        for (header in headers) {
                            addHeader(header.name, header.value)
                        }
                        removeHeader(ApiService.PLAY_INTEGRITY_HEADER)
                    }.build()
                )
            }

        }
    }

    private suspend fun getPlayIntegrityHeaders(request: Request): List<Header> {
        return try {
            val challenge = apiService.createAttestationChallenge().body()!!.challenge.base64Encoded
            listOf(
                Header(
                    ApiService.ATTESTATION_CHALLENGE_HEADER,
                    challenge
                ),
                Header(
                    ApiService.PLAY_INTEGRITY_TOKEN_HEADER,
                    playIntegrityRepository.getIntegrityToken(request.dataToSign(challenge).sha256digest().base64Encoded())
                )
            )
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.PlayIntegrity)
            listOf()
        }
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

fun Request.dataToSign(timestampOrChallenge: String): ByteArray {
    val requestPathAndQueryParams =
        this.url.encodedPath + (this.url.encodedQuery?.let { "?$it" } ?: "")
    val requestBody = this.body?.let {
        val buffer = Buffer()
        it.writeTo(buffer)
        buffer.readByteArray()
    } ?: byteArrayOf()

    return (this.method + requestPathAndQueryParams + Base64.getEncoder()
        .encodeToString(requestBody) + timestampOrChallenge).toByteArray()
}
