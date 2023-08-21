package co.censo.vault.data.networking

import android.os.Build
import co.censo.vault.BuildConfig
import co.censo.vault.data.model.CreateContactApiRequest
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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import java.time.Duration
import retrofit2.Response as RetrofitResponse

interface ApiService {

    companion object {

        const val IS_API = "X-IsApi"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"

        fun create(): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(AnalyticsInterceptor())
                .addInterceptor(AuthInterceptor())
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

class AuthInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request().newBuilder().build()
        //todo: manipulate request here as needed
        val response = chain.proceed(request)
        return response
    }
}
