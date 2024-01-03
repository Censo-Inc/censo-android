package co.censo.censo.presentation.import_phrase

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
    val getEncryptedResponse : Resource<GetImportEncryptedDataApiResponse> = Resource.Uninitialized,
    val exitFlow: Resource<Unit> = Resource.Uninitialized,
    val sendToSeedVerification: Resource<String> = Resource.Uninitialized
) {
    val error = acceptImportResource is Resource.Error || sendToSeedVerification is Resource.Error
}

sealed class ImportPhase {
    object None : ImportPhase()
    object Accepting : ImportPhase()
    data class Completing(val import: Import) : ImportPhase()
    data class Completed(val importedPhrase: ImportedPhrase) : ImportPhase()
}