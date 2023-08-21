package co.censo.vault.data

import java.lang.Exception

sealed class Resource<out T>(
    val data: T? = null,
    val exception: Exception? = null
) {
    object Uninitialized : Resource<Nothing>()
    class Success<out T>(data: T?) : Resource<T>(data)
    class Error<out T>(
        data: T? = null,
        exception: Exception? = null
    ) : Resource<T>(data, exception = exception)

    class Loading<out T>(data: T? = null) : Resource<T>(data)
}