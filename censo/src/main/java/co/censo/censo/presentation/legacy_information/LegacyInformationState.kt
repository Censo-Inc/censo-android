package co.censo.censo.presentation.legacy_information

import Base64EncodedData
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.BeneficiaryKeyInfo
import co.censo.shared.data.model.DecryptedApproverContactInfo
import co.censo.shared.data.model.DecryptedSeedPhraseNotes
import co.censo.shared.data.model.MasterKeyInfo
import co.censo.shared.data.model.OwnerApproverContactInfo
import co.censo.shared.data.model.OwnerApproverKeyInfo
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.model.UpdateBeneficiaryApproverContactInfoApiResponse
import co.censo.shared.data.model.UpdateSeedPhraseMetaInfoApiResponse
import co.censo.shared.util.NavigationData
import kotlinx.coroutines.CompletableDeferred


data class LegacyInformationState(
    //data
    val ownerData: OwnerData? = null,
    val ownerKeyCompletable: CompletableDeferred<OwnerApproverKeyInfo> = CompletableDeferred(),

    // ui state
    val uiState: UIState = UIState.Information,

    // api requests
    val retrieveKeyResource: Resource<Unit> = Resource.Uninitialized,
    val decryptContactInfoResource: Resource<Unit> = Resource.Uninitialized,
    val updateContactInfoResource: Resource<UpdateBeneficiaryApproverContactInfoApiResponse> = Resource.Uninitialized,
    val decryptSeedPhraseInfoResource: Resource<Unit> = Resource.Uninitialized,
    val updateSeedPhraseInfoResource: Resource<UpdateSeedPhraseMetaInfoApiResponse> = Resource.Uninitialized,


    // navigation
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized
) {

    val loading = decryptContactInfoResource is Resource.Loading
            || updateContactInfoResource is Resource.Loading
            || decryptSeedPhraseInfoResource is Resource.Loading
            || updateSeedPhraseInfoResource is Resource.Loading

    val asyncError = decryptContactInfoResource is Resource.Error
            || updateContactInfoResource is Resource.Error
            || decryptSeedPhraseInfoResource is Resource.Error
            || updateSeedPhraseInfoResource is Resource.Error
            || retrieveKeyResource is Resource.Error

    data class OwnerData(
        val participantId: ParticipantId,
        val entropy: Base64EncodedData,
        val seedPhrases: List<SeedPhrase>,
        val approvers: List<Approver.TrustedApprover>,
        val approverContactInfo: List<OwnerApproverContactInfo>,
        val beneficiaryKeyInfo: BeneficiaryKeyInfo,
        val masterKeyInfo: MasterKeyInfo
    )

    sealed class UIState {
        data object Information : UIState()
        data object InformationType : UIState()
        data class ApproverInformation(
            val contactInfo: List<DecryptedApproverContactInfo>
        ) : UIState()
        data class SelectSeedPhrase(
            val seedPhrases: List<SeedPhrase>
        ) : UIState()
        data class SeedPhraseInformation(
            val seedPhrase: DecryptedSeedPhraseNotes
        ) : UIState()
    }
}


