package co.censo.censo.presentation.beneficiary

import android.content.Context
import co.censo.censo.R
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.OwnerState

data class BeneficiaryState(
    val beneficiaryData: OwnerState.Beneficiary? = null,
    val verificationCode: String = "",
    val beneficiaryEncryptionKey: EncryptionKey? = null,
    //Used when a user has submitted verification code but we need to retrieve key or do send up the code
    val manuallyLoadingVerificationCode: Boolean = false,

    val apiResponse: Resource<Any> = Resource.Uninitialized,
    val error: BeneficiaryError? = null,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val retrievePrivateKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
)

enum class BeneficiaryError {
    MissingEssentialData, FailedSubmitCode, CloudError;

    fun errorToString(context: Context) =
        when (this) {
            FailedSubmitCode -> context.getString(R.string.beneficiary_failed_submit_code)
            CloudError -> context.getString(R.string.beneficiary_cloude_error)
            MissingEssentialData -> context.getString(R.string.missing_essential_beneficiary_data)
        }
}