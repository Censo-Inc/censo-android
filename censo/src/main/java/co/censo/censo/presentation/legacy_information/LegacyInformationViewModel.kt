package co.censo.censo.presentation.legacy_information

import SeedPhraseId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.data.model.DecryptedApproverContactInfo
import co.censo.shared.data.model.MasterKeyInfo
import co.censo.shared.data.model.OwnerApproverEncryptedKeyInfo
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.asResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegacyInformationViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
) : ViewModel() {

    var state: LegacyInformationState by mutableStateOf(LegacyInformationState())
        private set

    fun onStart() {
        viewModelScope.launch {
            onOwnerState(ownerRepository.getOwnerStateValue())
        }
        retrieveKeyFromTheCloud()
    }

    fun retrieveKeyFromTheCloud() {
        state = state.copy(retrieveKeyResource = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val keyRetrievalResult = keyRepository.retrieveKeyFromCloudAwaitPermissions(state.ownerData!!.participantId.value)
            if (keyRetrievalResult is Resource.Success) {
                state.ownerKeyCompletable.complete(
                    OwnerApproverEncryptedKeyInfo(
                        participantId = state.ownerData!!.participantId,
                        deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                        entropy = state.ownerData!!.entropy,
                        encryptedApproverKey = keyRetrievalResult.data,
                    )
                )
                state = state.copy(retrieveKeyResource = keyRetrievalResult.map { })
            } else {
                state = state.copy(retrieveKeyResource = Resource.Error())
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState) {
        val ownerStateReady = ownerState as? OwnerState.Ready
        val activatedBeneficiary: BeneficiaryStatus.Activated? = ownerStateReady?.policy?.beneficiary?.status as? BeneficiaryStatus.Activated

        if (ownerStateReady == null || activatedBeneficiary == null) {
            // navigate user out in case of incomplete data
            state = state.copy(navigationResource = Screen.OwnerVaultScreen.navTo().asResource())
            return
        }

        state = state.copy(
            ownerData = LegacyInformationState.OwnerData(
                entropy = ownerState.policy.ownerEntropy!!,
                participantId = ownerState.policy.owner?.participantId!!,
                seedPhrases = ownerState.vault.seedPhrases,
                approvers = ownerState.policy.approvers.filter { !it.isOwner },
                approverContactInfo = activatedBeneficiary.approverContactInfo,
                beneficiaryKeyInfo = activatedBeneficiary.beneficiaryKeyInfo,
                masterKeyInfo = MasterKeyInfo(
                    publicKey = ownerState.vault.publicMasterEncryptionKey,
                    keySignature = ownerState.policy.masterKeySignature!!
                )
            )
        )

        ownerRepository.updateOwnerState(ownerState)
    }

    fun moveUIToInformationType() {
        state = state.copy(uiState = LegacyInformationState.UIState.InformationType)
    }

    fun moveUIToApproverInfo() {
        state = state.copy(decryptContactInfoResource = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val ownerData = state.ownerData!!
            val approversContactInfo = ownerRepository.decryptApproverContactInfo(
                approvers = ownerData.approvers,
                approverContactInfo = ownerData.approverContactInfo,
                ownerApproverEncryptedKeyInfo = state.ownerKeyCompletable.await()
            )

            if (approversContactInfo is Resource.Success) {
                state = state.copy(
                    uiState = LegacyInformationState.UIState.ApproverInformation(
                        contactInfo = approversContactInfo.data
                    ),
                    decryptContactInfoResource = approversContactInfo.map { }
                )
            } else if (approversContactInfo is Resource.Error) {
                state = state.copy(decryptContactInfoResource = approversContactInfo.map { })
            }
        }
    }

    fun moveUIToSelectSeedPhrase() {
        state = state.copy(
            uiState = LegacyInformationState.UIState.SelectSeedPhrase(state.ownerData!!.seedPhrases)
        )
    }

    fun moveUIToSeedPhraseInformation(seedPhrase: SeedPhrase) {
        state = state.copy(decryptSeedPhraseInfoResource = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val seedPhraseInfo = ownerRepository.decryptSeedPhraseMetaInfo(
                seedPhrase = seedPhrase,
                ownerApproverEncryptedKeyInfo = state.ownerKeyCompletable.await(),
            )

            if (seedPhraseInfo is Resource.Success) {
                state = state.copy(
                    uiState = LegacyInformationState.UIState.SeedPhraseInformation(
                        seedPhraseInfo.data
                    ),
                    decryptSeedPhraseInfoResource = seedPhraseInfo.map { }
                )
            } else if (seedPhraseInfo is Resource.Error) {
                state = state.copy(decryptSeedPhraseInfoResource = seedPhraseInfo.map { })
            }
        }
    }

    fun saveApproverContactInformation(approverContactInfo: List<DecryptedApproverContactInfo>) {
        state = state.copy(updateContactInfoResource = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val ownerData = state.ownerData!!

            val updateResult = ownerRepository.updateBeneficiaryApproverContactInfo(
                clearTextApproverContacts = approverContactInfo.filter { it.contactInfo.isNotBlank() },
                ownerApproverEncryptedKeyInfo = state.ownerKeyCompletable.await(),
                beneficiaryKeyInfo = ownerData.beneficiaryKeyInfo,
                masterKeyInfo = ownerData.masterKeyInfo
            )

            if (updateResult is Resource.Success) {
                onOwnerState(updateResult.data.ownerState)
                state = state.copy(
                    updateContactInfoResource = updateResult,
                    uiState = LegacyInformationState.UIState.InformationType
                )
            } else if (updateResult is Resource.Error) {
                state = state.copy(updateContactInfoResource = updateResult)
            }
        }
    }

    fun saveSeedPhraseInformation(guid: SeedPhraseId, notes: String) {
        state = state.copy(updateSeedPhraseInfoResource = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val updateResult = when {
                notes.isNotBlank() -> ownerRepository.updateSeedPhraseNotes(
                    guid = guid,
                    notes = notes,
                    ownerApproverEncryptedKeyInfo = state.ownerKeyCompletable.await(),
                    masterKeyInfo = state.ownerData!!.masterKeyInfo
                )

                else -> ownerRepository.deleteSeedPhraseNotes(guid = guid)
            }

            if (updateResult is Resource.Success) {
                onOwnerState(updateResult.data.ownerState)
                state = state.copy(
                    uiState = LegacyInformationState.UIState.SelectSeedPhrase(state.ownerData!!.seedPhrases),
                    updateSeedPhraseInfoResource = updateResult,
                )
            } else if (updateResult is Resource.Error) {
                state = state.copy(updateSeedPhraseInfoResource = updateResult)
            }
        }
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun onTopBarBackClicked() {
        state = when (state.uiState) {
            LegacyInformationState.UIState.Information ->
                state.copy(navigationResource = Screen.OwnerVaultScreen.navTo().asResource())

            LegacyInformationState.UIState.InformationType ->
                state.copy(uiState = LegacyInformationState.UIState.Information)

            is LegacyInformationState.UIState.ApproverInformation ->
                state.copy(uiState = LegacyInformationState.UIState.InformationType)

            is LegacyInformationState.UIState.SelectSeedPhrase ->
                state.copy(uiState = LegacyInformationState.UIState.InformationType)

            is LegacyInformationState.UIState.SeedPhraseInformation ->
                state.copy(
                    uiState = LegacyInformationState.UIState.SelectSeedPhrase(
                        seedPhrases = state.ownerData!!.seedPhrases
                    )
                )
        }
    }

    fun resetDecryptContactInfoResource() {
        state = state.copy(decryptContactInfoResource = Resource.Uninitialized)
    }

    fun resetUpdateContactInfoResource() {
        state = state.copy(updateContactInfoResource = Resource.Uninitialized)
    }

    fun resetDecryptSeedPhraseInfoResource() {
        state = state.copy(decryptSeedPhraseInfoResource = Resource.Uninitialized)
    }

    fun resetUpdateSeedPhraseInfoResource() {
        state = state.copy(updateSeedPhraseInfoResource = Resource.Uninitialized)
    }
}