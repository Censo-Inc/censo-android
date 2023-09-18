package co.censo.shared.data.networking

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import InitBiometryVerificationApiResponse
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import co.censo.shared.BuildConfig
import co.censo.shared.data.Header
import co.censo.shared.data.HeadersSerializer
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.AcceptGuardianshipApiRequest
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ConfirmGuardianshipApiRequest
import co.censo.shared.data.model.ConfirmShardReceiptApiRequest
import co.censo.shared.data.model.CreateGuardianApiRequest
import co.censo.shared.data.model.CreateGuardianApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.SubmitBiometryVerificationApiRequest
import co.censo.shared.data.model.SubmitBiometryVerificationApiResponse
import co.censo.shared.data.model.GetPoliciesApiResponse
import co.censo.shared.data.model.GetPolicyApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.InviteGuardianApiRequest
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.UpdatePolicyApiRequest
import co.censo.shared.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.shared.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.shared.data.networking.ApiService.Companion.IS_API
import co.censo.shared.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.shared.data.storage.Storage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import retrofit2.Response as RetrofitResponse


interface ApiService {

    companion object {

        const val INVITATION_ID = "invitationId"
        const val INTERMEDIATE_KEY = "intermediateKey"
        const val PARTICIPANT_ID = "participantId"

        const val IS_API = "X-IsApi"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"

        fun create(storage: Storage, context: Context, versionCode: String): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(ConnectivityInterceptor(context))
                .addInterceptor(AnalyticsInterceptor(versionCode))
                .addInterceptor(AuthInterceptor(storage))
                .connectTimeout(Duration.ofSeconds(180))
                .readTimeout(Duration.ofSeconds(180))
                .callTimeout(Duration.ofSeconds(180))
                .writeTimeout(Duration.ofSeconds(180))


            if (BuildConfig.DEBUG) {
                val logger =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                client.addInterceptor(logger)
            }

            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client.build())
                .addConverterFactory(Json{ ignoreUnknownKeys = true }.asConverterFactory(contentType))
                .build()
                .create(ApiService::class.java)
        }
    }


    @POST("/v1/sign-in")
    suspend fun signIn(@Body signInApiRequest: SignInApiRequest): RetrofitResponse<ResponseBody>

    @GET("/v1/user")
    suspend fun user(): RetrofitResponse<GetUserApiResponse>

    @POST("/v1/biometry-verifications")
    suspend fun biometryVerification() : RetrofitResponse<InitBiometryVerificationApiResponse>

    @POST("/v1/biometry-verifications/{id}/biometry")
    suspend fun submitFacetecResult(
        @Path(value = "id", encoded = true) biometryId: String,
        @Body facetecResultRequest: SubmitBiometryVerificationApiRequest
    ) : RetrofitResponse<SubmitBiometryVerificationApiResponse>

    @POST("/v1/policies")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @PUT("/v1/policies/{$INTERMEDIATE_KEY}")
    suspend fun updatePolicy(
        @Path(value = INTERMEDIATE_KEY, encoded = true) intermediateKey: Base58EncodedPublicKey,
        @Body updatePolicyApiRequest: UpdatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @GET("/v1/policies/{$INTERMEDIATE_KEY}")
    suspend fun policy(
        @Path(value = INTERMEDIATE_KEY, encoded = true) intermediateKey: Base58EncodedPublicKey,
    ): RetrofitResponse<GetPolicyApiResponse>

    @GET("/v1/policies")
    suspend fun policies(): RetrofitResponse<GetPoliciesApiResponse>

    @POST("/v1/guardians")
    suspend fun createGuardian(
        @Body createGuardianApiRequest: CreateGuardianApiRequest
    ) : RetrofitResponse<CreateGuardianApiResponse>

    @POST("/v1/guardians/{$PARTICIPANT_ID}/invitation")
    suspend fun inviteGuardian(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body inviteGuardianApiRequest: InviteGuardianApiRequest
    ): RetrofitResponse<InviteGuardianApiResponse>

    @POST("/v1/guardianship-invitation/{$INVITATION_ID}/accept")
    suspend fun acceptGuardianship(
        @Path(value = INVITATION_ID) invitationId: String,
        @Body acceptGuardianshipApiRequest: AcceptGuardianshipApiRequest
    ): RetrofitResponse<AcceptGuardianshipApiResponse>

    @POST("/v1/guardianship-invitation/{$INVITATION_ID}/decline")
    suspend fun declineGuardianship(
        @Path(value = INVITATION_ID) invitationId: String,
    ): RetrofitResponse<ResponseBody>

    @POST("/v1/policies/{intermediateKey}/guardian/{$PARTICIPANT_ID}/shard-receipt-confirmation")
    suspend fun confirmShardReceipt(
        @Path(value = INTERMEDIATE_KEY, encoded = true) intermediateKey: String,
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body confirmShardReceiptApiRequest: ConfirmShardReceiptApiRequest
    ): RetrofitResponse<ResponseBody>

    @POST("/v1/policies/{intermediateKey}/guardian/{$PARTICIPANT_ID}/confirmation")
    suspend fun confirmGuardianship(
        @Path(value = INTERMEDIATE_KEY, encoded = true) intermediateKey: String,
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body confirmGuardianshipApiRequest: ConfirmGuardianshipApiRequest
    ): RetrofitResponse<ResponseBody>

    @POST("v1/notification-tokens")
    suspend fun addPushNotificationToken(@Body pushData: PushBody): RetrofitResponse<ResponseBody>

    @DELETE("v1/notification-tokens/{deviceType}")
    suspend fun removePushNotificationToken(
        @Path("deviceType") deviceType: String
    ) : RetrofitResponse<Unit>
}

class AnalyticsInterceptor(
    val versionCode: String
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

class AuthInterceptor(private val storage: Storage) : Interceptor {
    private val AUTHORIZATION_HEADER = "Authorization"
    private val DEVICE_PUBLIC_KEY_HEADER = "X-Censo-Device-Public-Key"
    private val TIMESTAMP_HEADER = "X-Censo-Timestamp"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val now = Clock.System.now()
        val headers = getAuthHeaders(now, request)

        return chain.proceed(
            request.newBuilder().apply {
                for (header in headers) {
                    addHeader(header.name, header.value)
                }
            }.build()
        )
    }

    private fun getAuthHeaders(now: Instant, request: Request): List<Header> {
        val deviceKey = InternalDeviceKey()
        val signature = Base64.getEncoder().encodeToString(deviceKey.sign(dataToSign(request, now)))

        return listOf(
            Header(AUTHORIZATION_HEADER, "signature $signature"),
            Header(DEVICE_PUBLIC_KEY_HEADER, deviceKey.publicExternalRepresentation().value),
            Header(TIMESTAMP_HEADER, now.toString())
        )
    }

    private fun dataToSign(request: Request, timestamp: Instant): ByteArray {
        val requestPathAndQueryParams = request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: "")
        val requestBody = request.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            buffer.readByteArray()
        } ?: byteArrayOf()

        return (request.method + requestPathAndQueryParams + Base64.getEncoder().encodeToString(requestBody) + timestamp.toString()).toByteArray()
    }
}

class ConnectivityInterceptor(private val context: Context) : Interceptor {

    private fun isOnline(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
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