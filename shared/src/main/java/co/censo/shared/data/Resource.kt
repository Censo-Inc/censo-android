package co.censo.shared.data

import android.content.Context
import co.censo.shared.R
import co.censo.shared.data.networking.NoConnectivityException
import co.censo.shared.data.repository.ErrorResponse
import com.google.android.gms.common.api.ApiException
import java.lang.Exception

sealed class Resource<out T> {

    object Uninitialized : Resource<Nothing>()

    object Loading : Resource<Nothing>()

    class Success<out T>(val data: T) : Resource<T>()

    class Error<out T>(
        val exception: Exception? = null,
        val errorResponse: ErrorResponse? = null,
        val errorCode: Int? = null
    ) : Resource<T>() {
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
                426 -> context.getString(R.string.upgrade_required)
                else -> "${context.getString(R.string.unexpected_error)} ${errorCode?.let { "($it)"} ?: exception?.message}"
            }
        }
    }

    fun <K> map(f: (T) -> K): Resource<K> {
        return when (this) {
            is Uninitialized -> Uninitialized
            is Loading -> Loading
            is Success -> Success(f(this.data))
            is Error -> Error(this.exception, this.errorResponse, this.errorCode)
        }
    }

    fun <K> flatMap(f: (T) -> Resource<K>): Resource<K> {
        return when (this) {
            is Uninitialized -> Uninitialized
            is Loading -> Loading
            is Success -> f(this.data)
            is Error -> Error(this.exception, this.errorResponse, this.errorCode)
        }
    }

    suspend fun <K> flatMapSuspend(f: suspend (T) -> Resource<K>): Resource<K> {
        return when (this) {
            is Uninitialized -> Uninitialized
            is Loading -> Loading
            is Success -> f(this.data)
            is Error -> Error(this.exception, this.errorResponse, this.errorCode)
        }
    }

    fun onSuccess(success: (T) -> Unit) {
        if (this is Success) {
            success(this.data)
        }
    }

    fun success(): Success<T>? {
        return this as? Success<T>
    }

    fun asSuccess(): Success<T> {
        return this as Success<T>
    }

    fun error(): Error<T>? {
        return this as? Error<T>
    }

    fun asError(): Error<T> {
        return this as Error<T>
    }
}