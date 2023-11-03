package co.censo.censo.presentation.welcome

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState

data class WelcomeState(
    val ownerStateResource: Resource<OwnerState> = Resource.Uninitialized,
    val currentStep: WelcomeStep = WelcomeStep.Authenticated
) {
    val loading = ownerStateResource is Resource.Loading
    val asyncError = ownerStateResource is Resource.Error
}

enum class WelcomeStep(val order: Int) {
    Authenticated(1), FaceScanned(2), PhraseEntered(3)
}
