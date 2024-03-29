package co.censo.censo.presentation.import_phrase

import android.content.Context
import co.censo.censo.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetImportEncryptedDataApiResponse
import co.censo.shared.data.model.Import
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class PhraseImportState(
    val importPhraseState: ImportPhase = ImportPhase.None,
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val acceptImportResource: Resource<Unit> = Resource.Uninitialized,
    val getEncryptedResponse: Resource<GetImportEncryptedDataApiResponse> = Resource.Uninitialized,
    val exitFlow: Resource<Unit> = Resource.Uninitialized,
    val sendToSeedVerification: Resource<String> = Resource.Uninitialized,
    val importErrorType: ImportErrorType = ImportErrorType.NONE
) {
    val error = acceptImportResource is Resource.Error
            || sendToSeedVerification is Resource.Error
            || importErrorType != ImportErrorType.NONE
            || userResponse is Resource.Error
}

sealed class ImportPhase {
    object None : ImportPhase()
    object Accepting : ImportPhase()
    data class Completing(val import: Import) : ImportPhase()
    object Completed : ImportPhase()
}

enum class ImportErrorType {
    BAD_SIGNATURE, LINK_EXPIRED, LINK_IN_FUTURE, NONE;

    fun getErrorMessage(context: Context) =
        when {
            this == LINK_IN_FUTURE || this == BAD_SIGNATURE -> context.getString(R.string.invalid_link)
            this == LINK_EXPIRED -> context.getString(R.string.link_expired)
            else -> context.getString(R.string.something_went_wrong)
        }
}

fun isLinkExpired(timestamp: Long): ImportErrorType {
    val linkCreationTime = Instant.fromEpochMilliseconds(timestamp)
    val linkValidityStart = linkCreationTime.minus(10.seconds)
    val linkValidityEnd = linkCreationTime.plus(10.minutes)
    val now = Clock.System.now()

    return if (now > linkValidityEnd) {
        ImportErrorType.LINK_EXPIRED
    } else if (now < linkValidityStart) {
        ImportErrorType.LINK_IN_FUTURE
    } else {
        ImportErrorType.NONE
    }
}