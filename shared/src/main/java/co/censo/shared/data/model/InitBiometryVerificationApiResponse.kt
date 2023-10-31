import co.censo.shared.data.model.BiometryVerificationId
import kotlinx.serialization.Serializable

@Serializable
data class InitBiometryVerificationApiResponse(
    val id: BiometryVerificationId,
    val sessionToken: String,
    val productionKeyText: String,
    val deviceKeyId: String,
    val biometryEncryptionPublicKey: String
)