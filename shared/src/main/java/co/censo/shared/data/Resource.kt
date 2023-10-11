package co.censo.shared.data

import android.content.Context
import co.censo.shared.R
import co.censo.shared.data.networking.NoConnectivityException
import co.censo.shared.data.repository.ErrorResponse
import com.google.android.gms.common.api.ApiException
import java.lang.Exception

sealed class Resource<out T>(
    val data: T? = null,
    val exception: Exception? = null,
    val errorResponse: ErrorResponse? = null,
    val errorCode: Int? = null
) {
    object Uninitialized : Resource<Nothing>()
    class Success<out T>(data: T?) : Resource<T>(data)
    class Error<out T>(
        data: T? = null,
        exception: Exception? = null,
        errorResponse: ErrorResponse? = null,
        errorCode: Int? = null
    ) : Resource<T>(data, exception = exception, errorResponse = errorResponse, errorCode = errorCode) {
        fun getErrorMessage(context: Context): String {
            if (exception is NoConnectivityException) {
                return "Network NA"
            }
            if (exception is ApiException) {
                return exception.message ?: "Google API Error"
            }

            return when (errorCode) {
                400 -> context.getString(R.string.invalid_request)
                401 -> context.getString(R.string.invalid_request_signature)
                403 -> context.getString(R.string.unauthorized_access)
                418 -> context.getString(R.string.under_maintenance)
                422 -> this.errorResponse?.errors?.get(0)?.displayMessage
                        ?: this.errorResponse?.errors?.get(0)?.message
                        ?: context.getString(R.string.validation_error)
                else -> "${context.getString(R.string.unexpected_error)} ${errorCode?.let { "($it)"}}"
            }
        }

    }

    class Loading<out T>(data: T? = null) : Resource<T>(data)

    fun <K> map(f: (T) -> K): Resource<K> {
        return when (this) {
            is Success -> Success(this.data?.let { f(it) })
            is Error -> Error(this.data?.let { f(it) }, this.exception, this.errorResponse, this.errorCode)
            is Uninitialized -> Uninitialized
            is Loading -> Loading(this.data?.let { f(it) })
        }
    }
}