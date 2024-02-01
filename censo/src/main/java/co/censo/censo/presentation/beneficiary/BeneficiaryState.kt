package co.censo.censo.presentation.beneficiary

import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class BeneficiaryState(
    val beneficiaryData: OwnerState.Beneficiary? = null,
    val verificationCode: String = "",
    val beneficiaryEncryptionKey: EncryptionKey? = null,
    //Used when a user has submitted verification code but we need to retrieve key or do send up the code
    val manuallyLoadingVerificationCode: Boolean = false,

    val apiResponse: Resource<Any> = Resource.Uninitialized,
    val error: String? = null,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val retrievePrivateKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
)