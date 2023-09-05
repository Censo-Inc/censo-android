package co.censo.vault.data.repository

import co.censo.vault.data.Resource
import co.censo.vault.data.networking.NoConnectivityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response
import kotlinx.serialization.decodeFromString
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.FormattableFlags

abstract class BaseRepository {

    suspend fun <T> retrieveApiResource(apiToBeCalled: suspend () -> Response<T>): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<T> = apiToBeCalled()

                if (response.isSuccessful) {
                    Resource.Success(data = response.body())
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


    companion object {
        const val HTTP_404 = 404
        const val HTTP_401 = 401
    }
}

@Serializable
data class ErrorResponse(val errors: List<ErrorInfo>) {
    companion object {
        fun fromJson(json: String): ErrorResponse {
            return Json { ignoreUnknownKeys = true }.decodeFromString(json)
        }
    }
}

@Serializable
data class ErrorInfo(
    val reason: String? = null,
    val message: String? = null,
    val displayMessage: String? = null
)