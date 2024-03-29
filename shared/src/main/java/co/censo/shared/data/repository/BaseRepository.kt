package co.censo.shared.data.repository

import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.networking.IgnoreKeysJson.baseKotlinXJson
import co.censo.shared.data.networking.NoConnectivityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import retrofit2.Response

abstract class BaseRepository {

    suspend fun <T> retrieveApiResource(apiToBeCalled: suspend () -> Response<T>): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<T> = apiToBeCalled()

                if (response.isSuccessful) {
                    // 'null' body is possible in following cases:
                    // a) response.isSuccessful is false
                    // b) request method does not support response body (e.g. DELETE).
                    // c) retrofit was instructed to return Void type
                    // Relevant are only b) and c). Both imply that response won't be read or used.
                    Resource.Success(data = response.body() ?: Unit as T)
                } else {
                    Resource.Error(
                        errorResponse = getErrorInfoFromResponse(response),
                        exception = Exception("Request failed with code: ${response.code()}"),
                        errorCode = response.code()
                    )
                }
            } catch (e: NoConnectivityException) {
                Resource.Error(
                    exception = e
                )
            } catch (e: Exception) {
                Resource.Error(
                    exception = e
                )
            }
        }
    }

    private fun <T> getErrorInfoFromResponse(response: Response<T>) =
        try {
            response.errorBody()?.string()?.let {
                ErrorResponse.fromJson(it)

            }
        } catch (e: Exception) {
            null
        }
}

@Serializable
data class ErrorResponse(val errors: List<ErrorInfo>) {
    companion object {
        fun fromJson(json: String): ErrorResponse {
            return baseKotlinXJson.decodeFromString(json)
        }
    }
}

@Serializable
data class ErrorInfo(
    val reason: String? = null,
    val message: String? = null,
    val displayMessage: String? = null,
    val scanResultBlob: BiometryScanResultBlob? = null,
)