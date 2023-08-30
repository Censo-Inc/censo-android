package co.censo.vault.data.cryptography

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import co.censo.vault.BuildConfig
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.networking.AuthInterceptor
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Instant
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyStore
import java.security.*
import java.security.cert.Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.Cipher

interface CryptographyManager {
    fun deviceKeyExists(): Boolean
    fun deleteDeviceKeyIfPresent()
    fun getCertificateFromKeystore(): Certificate
    fun createDeviceKeyIfNotExists()
    fun getOrCreateDeviceKey(): PrivateKey
    fun getPublicKeyFromDeviceKey(): PublicKey
    fun getDevicePublicKeyInBase58(): String
    fun signData(dataToSign: ByteArray): ByteArray
    fun decryptData(ciphertext: ByteArray): ByteArray
    fun encryptData(plainText: String): ByteArray
    fun createAuthHeaders(now: Instant): AuthInterceptor.AuthHeadersWithTimestamp
}

class CryptographyManagerImpl : CryptographyManager {
    companion object {
        const val STATIC_DEVICE_KEY_CHECK = "STATIC_DEVICE_KEY_CHECK"

        const val KEY_ID = "deviceKey"

        const val BIOMETRY_TIMEOUT = 15
        const val KEY_SIZE: Int = 256
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val SECP_256_R1 = "secp256r1"
        const val SHA_256_ECDSA = "SHA256withECDSA"

        //AES Specific
        const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    }

    override fun createAuthHeaders(now: Instant): AuthInterceptor.AuthHeadersWithTimestamp {
        val iso8601FormattedTimestamp = now.toString()
        val signature = Base64.getEncoder()
            .encodeToString(signData(iso8601FormattedTimestamp.toByteArray()))
        val headers = ApiService.getAuthHeaders(
            signature,
            getDevicePublicKeyInBase58(),
            iso8601FormattedTimestamp
        )
        return AuthInterceptor.AuthHeadersWithTimestamp(headers, now)
    }

    override fun signData(dataToSign: ByteArray): ByteArray {
        val key = getOrCreateDeviceKey()
        val signature = Signature.getInstance(SHA_256_ECDSA)
        signature.initSign(key)
        signature.update(dataToSign)
        val signedData = signature.sign()

        val verified = verifySignature(
            dataSigned = dataToSign,
            signatureToCheck = signedData
        )

        if (!verified) {
            throw Exception("Device key signature invalid.")
        }

        return signedData
    }

    override fun decryptData(ciphertext: ByteArray): ByteArray {
        val key = getOrCreateDeviceKey()

        return ECIESManager.decryptMessage(
            cipherData = ciphertext,
            privateKey = key
        )
    }

    override fun encryptData(plainText: String): ByteArray {
        val publicKey = getPublicKeyFromDeviceKey()

        val compressedKey = ECIESManager.extractUncompressedPublicKey(publicKey.encoded)

        return ECIESManager.encryptMessage(
            dataToEncrypt = plainText.toByteArray(Charsets.UTF_8),
            publicKeyBytes = compressedKey,
        )
    }

    private fun verifySignature(
        dataSigned: ByteArray,
        signatureToCheck: ByteArray
    ): Boolean {
        val signature = Signature.getInstance(SHA_256_ECDSA)
        val publicKey = getPublicKeyFromDeviceKey()
        signature.initVerify(publicKey)
        signature.update(dataSigned)
        return signature.verify(signatureToCheck)
    }

    override fun createDeviceKeyIfNotExists() {
        getOrCreateDeviceKey()
    }

    override fun getOrCreateDeviceKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val key = keyStore.getKey(KEY_ID, null)
        if (key != null) return key as PrivateKey

        return createECDeviceKey()
    }

    override fun getPublicKeyFromDeviceKey(): PublicKey {
        val certificate = getCertificateFromKeystore()
        return certificate.publicKey
    }

    override fun getDevicePublicKeyInBase58(): String =
        Base58.base58Encode(
            BCECPublicKey(
                getPublicKeyFromDeviceKey() as ECPublicKey,
                BouncyCastleProvider.CONFIGURATION
            ).q.getEncoded(true)
        )

    override fun getCertificateFromKeystore(): Certificate {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        return keyStore.getCertificate(KEY_ID)
    }

    private fun createECDeviceKey(): PrivateKey {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val paramBuilder = KeyGenParameterSpec.Builder(
            KEY_ID,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_AGREE_KEY
        )

        val parameterSpec = paramBuilder
            .setAlgorithmParameterSpec(ECGenParameterSpec(SECP_256_R1))
            .setKeySize(KEY_SIZE)
            .setIsStrongBoxBacked(BuildConfig.STRONGBOX_ENABLED)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                BIOMETRY_TIMEOUT, KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .setInvalidatedByBiometricEnrollment(true)
            .setDigests(
                KeyProperties.DIGEST_SHA256
            )
            .build()

        kpg.initialize(parameterSpec)

        return kpg.generateKeyPair().private
    }

    override fun deleteDeviceKeyIfPresent() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val haveKey = keyStore.containsAlias(KEY_ID)

        if (haveKey) {
            keyStore.deleteEntry(KEY_ID)
        }
    }

    override fun deviceKeyExists(): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(KEY_ID)
    }

    private fun getAESCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }
}

data class EncryptedData(val ciphertext: ByteArray, val initializationVector: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!initializationVector.contentEquals(other.initializationVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + initializationVector.contentHashCode()
        return result
    }
}
