import kotlinx.serialization.Serializable

@Serializable
data class InitBiometryVerificationApiResponse(
    val id: String,
    val sessionToken: String,
    val productionKeyText: String,
    val deviceKeyId: String,
    val biometryEncryptionPublicKey: String,
    val firstTime: Boolean
)