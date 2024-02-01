package co.censo.censo.presentation.beneficiary_owner

import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.InviteBeneficiaryApiResponse
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock

data class BeneficiaryOwnerState(
    //Owner Specific
    val ownerState: OwnerState.Ready? = null,
    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,

    //Beneficiary Specific
    val beneficiaryLabel: String = "",
    val createBeneficiaryError: String? = null,
    val inviteBeneficiaryResource: Resource<InviteBeneficiaryApiResponse> = Resource.Uninitialized,
    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val beneficiaryCode: String = "",
    val beneficiaryOwnerUIState: BeneficiaryOwnerUIState = BeneficiaryOwnerUIState.None
)

fun BeneficiaryStatus?.toBeneficiaryOwnerUIState() =
    when (this) {
        null -> BeneficiaryOwnerUIState.None

        is BeneficiaryStatus.Initial,
        is BeneficiaryStatus.Accepted,
        is BeneficiaryStatus.VerificationSubmitted -> BeneficiaryOwnerUIState.ActivateBeneficiary

        is BeneficiaryStatus.Activated -> BeneficiaryOwnerUIState.BeneficiaryActive
    }

enum class BeneficiaryOwnerUIState {
    None, AddLabel, VerifyNow, ActivateBeneficiary, BeneficiaryActive
}