package co.censo.shared.data

import android.content.Context
import co.censo.shared.data.networking.NoConnectivityException
import co.censo.shared.data.repository.ErrorResponse
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

            val displayMessage = this.errorResponse?.errors?.get(0)?.displayMessage
            val errorMessage = this.errorResponse?.errors?.get(0)?.message
            val exceptionMessage = this.exception?.message

            return if (!displayMessage.isNullOrEmpty()) {
                displayMessage
            } else if (!errorMessage.isNullOrEmpty()) {
                errorMessage
            } else if (!exceptionMessage.isNullOrEmpty()) {
                exceptionMessage
            } else {
                "Default error message"
            }
        }

    }

    class Loading<out T>(data: T? = null) : Resource<T>(data)
}