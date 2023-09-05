import kotlinx.serialization.Serializable

@Serializable
data class InitBiometryVerificationApiResponse(
    val sessionToken: String,
    val deviceKeyId: String,
    val biometryEncryptionPublicKey: String,
    val firstTime: Boolean
)