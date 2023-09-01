package co.censo.vault.data.networking

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.security.keystore.UserNotAuthenticatedException
import co.censo.vault.AuthHeadersState
import co.censo.vault.BuildConfig
import co.censo.vault.data.Header
import co.censo.vault.data.HeadersSerializer
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.model.CreateContactApiRequest
import co.censo.vault.data.model.CreateContactApiResponse
import co.censo.vault.data.model.CreatePolicyApiRequest
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.GetPoliciesApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.Policy
import co.censo.vault.data.model.UpdatePolicyApiRequest
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.vault.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.vault.data.networking.ApiService.Companion.IS_API
import co.censo.vault.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.vault.data.repository.PushBody
import co.censo.vault.data.storage.Storage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.IOException
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import retrofit2.Response as RetrofitResponse


interface ApiService {

    companion object {

        const val IS_API = "X-IsApi"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val DEVICE_PUBLIC_KEY_HEADER = "X-Censo-Device-Public-Key"
        const val TIMESTAMP_HEADER = "X-Censo-Timestamp"

        fun getAuthHeaders(
            base64FormattedSignature: String,
            base58FormattedDevicePublicKey: String,
            iso8601FormattedTimestamp: String
        ): List<Header> =
            listOf(
                Header(AUTHORIZATION_HEADER, "signature $base64FormattedSignature"),
                Header(DEVICE_PUBLIC_KEY_HEADER, base58FormattedDevicePublicKey),
                Header(TIMESTAMP_HEADER, iso8601FormattedTimestamp)
            )

        fun create(cryptographyManager: CryptographyManager, storage: Storage, context: Context): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(ConnectivityInterceptor(context))
                .addInterceptor(AnalyticsInterceptor())
                .addInterceptor(AuthInterceptor(cryptographyManager, storage))
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


    @POST("/v1/device")
    suspend fun createDevice():
            RetrofitResponse<ResponseBody>

    @POST("/v1/user")
    suspend fun createUser(@Body createUserApiRequest: CreateUserApiRequest):
            RetrofitResponse<ResponseBody>

    @GET("/v1/user")
    suspend fun user(): RetrofitResponse<GetUserApiResponse>

    @POST("/v1/contacts")
    suspend fun createContact(@Body createContactApiRequest: CreateContactApiRequest): RetrofitResponse<CreateContactApiResponse>

    @POST("/v1/verifications/{id}/code")
    suspend fun verifyContact(
        @Path(value = "id", encoded = true) verificationId: String,
        @Body verifyContactApiRequest: VerifyContactApiRequest
    ): RetrofitResponse<ResponseBody>

    @POST("/v1/policies")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @PUT("/v1/policies")
    suspend fun updatePolicy(
        @Body updatePolicyApiRequest: UpdatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @GET("/v1/policies")
    suspend fun policy(): RetrofitResponse<Policy>

    @GET("/v1/policies")
    suspend fun policies(): RetrofitResponse<GetPoliciesApiResponse>

    @DELETE("/v1/policies")
    suspend fun cancelPolicy() : RetrofitResponse<Unit>

    @POST("/v1/policy/encrypted-data")
    suspend fun storeEncryptedPhraseData(): RetrofitResponse<ResponseBody>

    @POST("v1/notification-tokens")
    suspend fun addPushNotificationToken(@Body pushData: PushBody): RetrofitResponse<ResponseBody>

    @DELETE("v1/notification-tokens/{deviceType}")
    suspend fun removePushNotificationToken(
        @Path("deviceType") deviceType: String
    ) : RetrofitResponse<Unit>
}

data class HoldingBody(val ok: String)


class AnalyticsInterceptor : Interceptor {
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
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
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
    private val cryptographyManager: CryptographyManager,
    private val storage: Storage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val now = Clock.System.now()
        val headers = getAuthHeaders(now)

        return chain.proceed(
            request.newBuilder().apply {
                headers?.let {
                    for (header in headers) {
                        addHeader(header.name, header.value)
                    }
                }
            }.build()
        )
    }

    @Serializable
    data class AuthHeadersWithTimestamp(
        @Serializable(with = HeadersSerializer::class)
        val headers: List<Header>,
        val createdAt: Instant
    ) {
        fun isExpired(now: Instant): Boolean =
            createdAt + 1.days - 1.minutes <= now
    }

    private var cachedReadCallHeaders: AuthHeadersWithTimestamp? = storage.retrieveReadHeaders()

    private fun getAuthHeaders(now: Instant): List<Header>? {
        val cachedHeaders = storage.retrieveReadHeaders()
        return if (cachedHeaders == null || cachedHeaders.isExpired(now)) {
            storage.clearReadHeaders()
            try {
                cachedReadCallHeaders = cryptographyManager.createAuthHeaders(now)
                cachedReadCallHeaders?.let { storage.saveReadHeaders(it) }
                storage.setAuthHeadersState(AuthHeadersState.VALID)
                cachedReadCallHeaders?.headers
            } catch (e: UserNotAuthenticatedException) {
                //User does not have biometry to complete the signing
                storage.setAuthHeadersState(AuthHeadersState.MISSING)
                null
            }
        } else {
            cachedHeaders.headers
        }
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