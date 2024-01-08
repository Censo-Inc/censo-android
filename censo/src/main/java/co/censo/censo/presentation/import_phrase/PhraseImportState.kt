package co.censo.censo.presentation.import_phrase

import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetImportEncryptedDataApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.Import
import co.censo.shared.data.model.ImportedPhrase
import co.censo.shared.data.model.OwnerState

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
            this == LINK_IN_FUTURE -> "This link is invalid, please get a new one."
            this == LINK_EXPIRED -> "This link has expired, please get a new one."
            this == BAD_SIGNATURE -> "This link is invalid, please get a new one."
            else -> "Something went wrong"
        }
}