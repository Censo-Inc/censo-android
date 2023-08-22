package co.censo.vault.data.networking

import android.os.Build
import co.censo.vault.BuildConfig
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.model.CreateContactApiRequest
import co.censo.vault.data.model.CreatePolicyApiRequest
import co.censo.vault.data.model.CreateUserApiRequest
import co.censo.vault.data.model.GetPoliciesApiResponse
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.data.model.Policy
import co.censo.vault.data.model.UpdatePolicyApiRequest
import co.censo.vault.data.model.VerifyContactApiRequest
import co.censo.vault.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.vault.data.networking.ApiService.Companion.AUTHORIZATION_HEADER
import co.censo.vault.data.networking.ApiService.Companion.DEVICE_PUBLIC_KEY_HEADER
import co.censo.vault.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.vault.data.networking.ApiService.Companion.IS_API
import co.censo.vault.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.vault.data.networking.ApiService.Companion.TIMESTAMP_HEADER
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
import java.time.Duration
import java.util.Base64
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

        fun create(cryptographyManager: CryptographyManager): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(AnalyticsInterceptor())
                .addInterceptor(AuthInterceptor(cryptographyManager))
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
                .addConverterFactory(Json.asConverterFactory(contentType))
                .build()
                .create(ApiService::class.java)
        }
    }


    @POST("/user")
    suspend fun createUser(@Body createUserApiRequest: CreateUserApiRequest):
            RetrofitResponse<ResponseBody>

    @GET("/user")
    suspend fun user(): RetrofitResponse<GetUserApiResponse>

    @POST("/contacts")
    suspend fun createContact(@Body createContactApiRequest: CreateContactApiRequest): RetrofitResponse<ResponseBody>

    @POST("/contacts/{contact_id}/verification-code")
    suspend fun verifyContact(
        @Path("contact_id") contactId: String,
        @Body verifyContactApiRequest: VerifyContactApiRequest
    ): RetrofitResponse<ResponseBody>

    @POST("/policies")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @PUT("/policies")
    suspend fun updatePolicy(
        @Body updatePolicyApiRequest: UpdatePolicyApiRequest
    ): RetrofitResponse<ResponseBody>

    @GET("/policies")
    suspend fun policy(): RetrofitResponse<Policy>

    @GET("/policies")
    suspend fun policies(): RetrofitResponse<GetPoliciesApiResponse>

    @DELETE
    suspend fun cancelPolicy() : RetrofitResponse<Unit>

    @POST("/policy/encrypted-data")
    suspend fun storeEncryptedPhraseData(): RetrofitResponse<ResponseBody>
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
    private val cryptographyManager: CryptographyManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val now = Clock.System.now()
        val headers = when (request.method) {
            "GET", "HEAD", "OPTIONS", "TRACE" -> getReadCallAuthHeaders(now)
            else -> getWriteCallAuthHeaders(now, request)
        }

        return chain.proceed(
            request.newBuilder().apply {
                for ((name, value) in headers) {
                    addHeader(name, value)
                }
            }.build()
        )
    }

    private data class AuthHeadersWithTimestamp(
        val headers: Headers,
        val createdAt: Instant
    ) {
        fun isExpired(now: Instant): Boolean =
            createdAt + 1.days - 1.minutes <= now
    }

    private var cachedReadCallHeaders: AuthHeadersWithTimestamp? = null

    private fun getReadCallAuthHeaders(now: Instant): Headers {
        val cachedHeaders = cachedReadCallHeaders
        return if (cachedHeaders == null || cachedHeaders.isExpired(now)) {
            val iso8601FormattedTimestamp = now.toString()
            val signature = Base64.getEncoder().encodeToString(cryptographyManager.signData(iso8601FormattedTimestamp.toByteArray()))
            val headers = getAuthHeaders(signature, cryptographyManager.getDevicePublicKeyInBase58(), iso8601FormattedTimestamp)
            cachedReadCallHeaders = AuthHeadersWithTimestamp(headers, now)
            headers
        } else {
            cachedHeaders.headers
        }
    }

    private fun getWriteCallAuthHeaders(now: Instant, request: Request): Headers {
        val httpMethod = request.method
        val pathAndQueryParams = request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: "")
        val bodyBase64Encoded = Base64.getEncoder().encodeToString(
            request.body?.let {
                val buffer = Buffer()
                it.writeTo(buffer)
                buffer.readByteArray()
            } ?: byteArrayOf()
        )
        val iso8601FormattedTimestamp = now.toString()
        val stringToSign = httpMethod + pathAndQueryParams + bodyBase64Encoded + iso8601FormattedTimestamp
        val signature = Base64.getEncoder().encodeToString(cryptographyManager.signData(stringToSign.toByteArray()))
        return getAuthHeaders(signature, cryptographyManager.getDevicePublicKeyInBase58(), iso8601FormattedTimestamp)
    }

    private fun getAuthHeaders(base64FormattedSignature: String, base58FormattedDevicePublicKey: String, iso8601FormattedTimestamp: String): Headers =
        Headers.Builder()
            .add(AUTHORIZATION_HEADER, "signature $base64FormattedSignature")
            .add(DEVICE_PUBLIC_KEY_HEADER, base58FormattedDevicePublicKey)
            .add(TIMESTAMP_HEADER, iso8601FormattedTimestamp)
            .build()

}
