package co.censo.vault.data.cryptography.key

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import co.censo.vault.BuildConfig
import co.censo.vault.data.cryptography.ECIESManager
import io.github.novacrypto.base58.Base58
import java.math.BigInteger
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.networking.AuthInterceptor
import kotlinx.datetime.Instant
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

interface VaultKey {

    fun publicKeyUncompressed(): ByteArray
    fun publicExternalRepresentation(): Base58EncodedPublicKey
    fun privateKeyRaw(): ByteArray
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
    fun sign(data: ByteArray): ByteArray
    fun verify(signedData: ByteArray, signature: ByteArray): Boolean
}

class EncryptionKey(val key: KeyPair) : VaultKey {

    override fun publicExternalRepresentation(): Base58EncodedPublicKey {
        return Base58EncodedDevicePublicKey(Base58.base58Encode(publicKeyUncompressed()))
    }

    override fun publicKeyUncompressed() =
        ECIESManager.extractUncompressedPublicKey(key.public.encoded)

    override fun privateKeyRaw(): ByteArray =
        (key.private as ECPrivateKey).s.toByteArray()

    override fun encrypt(data: ByteArray): ByteArray =
        ECIESManager.encryptMessage(data, publicKeyUncompressed())

    override fun decrypt(data: ByteArray): ByteArray =
        ECIESManager.decryptMessage(data, key.private)

    override fun sign(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun verify(signedData: ByteArray, signature: ByteArray): Boolean {
        TODO("Not yet implemented")
    }


    companion object {
        fun generateFromPrivateKeyRaw(raw: BigInteger): EncryptionKey {
            val privateKey = ECIESManager.getPrivateKeyFromECBigIntAndCurve(raw) as ECPrivateKey
            val publicKey = ECIESManager.getPublicKeyFromPrivateKey(privateKey)

            return EncryptionKey(KeyPair(publicKey, privateKey))
        }

        fun generateRandomKey(): EncryptionKey =
            EncryptionKey(ECIESManager.createSecp256R1KeyPair())
    }

}

/**
 * This will represent a SECP256R1 key, currently in the system this is the Master Key and Intermediate Key
 *
 * SelfDeviceKey will be able to sign/verify data and encrypt/decrypt data.
 *
 * OtherDeviceKey will be able to encrypt data and verify data. It will not be able to decrypt or sign data.
 *
 */
class InternalDeviceKey() : VaultKey {

    private val keystoreHelper = KeystoreHelper()

    val key = keystoreHelper.getOrCreateDeviceKey()

    companion object {
        const val STATIC_DEVICE_KEY_CHECK = "STATIC_DEVICE_KEY_CHECK"
        const val KEY_ID = "deviceKey"
    }

    override fun publicKeyUncompressed(): ByteArray {
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey()
        return ECIESManager.extractUncompressedPublicKey(publicKey.encoded)
    }

    override fun publicExternalRepresentation(): Base58EncodedPublicKey {
        return Base58EncodedDevicePublicKey(keystoreHelper.getDevicePublicKeyInBase58())
    }

    override fun privateKeyRaw(): ByteArray {
        throw Exception("Cannot access private key on Keystore Key")
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey()

        val compressedKey = ECIESManager.extractUncompressedPublicKey(publicKey.encoded)

        return ECIESManager.encryptMessage(
            dataToEncrypt = data,
            publicKeyBytes = compressedKey,
        )
    }

    override fun decrypt(data: ByteArray): ByteArray {
        return ECIESManager.decryptMessage(
            cipherData = data,
            privateKey = key
        )
    }

    override fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(KeystoreHelper.SHA_256_ECDSA)
        signature.initSign(key)
        signature.update(data)
        val signedData = signature.sign()

        val verified = verify(
            signedData = data,
            signature = signedData
        )

        if (!verified) {
            throw Exception("Device key signature invalid.")
        }

        return signedData
    }

    override fun verify(signedData: ByteArray, signature: ByteArray): Boolean {
        val signatureKeystore = Signature.getInstance(KeystoreHelper.SHA_256_ECDSA)
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey()
        signatureKeystore.initVerify(publicKey)
        signatureKeystore.update(signedData)
        return signatureKeystore.verify(signature)
    }

    fun removeKey() = keystoreHelper.deleteDeviceKeyIfPresent()

    fun createAuthHeaders(now: Instant): AuthInterceptor.AuthHeadersWithTimestamp {
        val iso8601FormattedTimestamp = now.toString()
        val signature = Base64.getEncoder()
            .encodeToString(sign(iso8601FormattedTimestamp.toByteArray()))
        val headers = ApiService.getAuthHeaders(
            signature,
            Base58EncodedDevicePublicKey(publicExternalRepresentation().value),
            iso8601FormattedTimestamp
        )
        return AuthInterceptor.AuthHeadersWithTimestamp(headers, now)
    }

    internal class KeystoreHelper {
        companion object {
            const val BIOMETRY_TIMEOUT = 15
            const val KEY_SIZE: Int = 256
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val SECP_256_R1 = "secp256r1"
            const val SHA_256_ECDSA = "SHA256withECDSA"
        }

        fun getOrCreateDeviceKey(): PrivateKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val key = keyStore.getKey(KEY_ID, null)
            if (key != null) return key as PrivateKey

            return createECDeviceKey()
        }

        fun getPublicKeyFromDeviceKey(): PublicKey {
            val certificate = getCertificateFromKeystore()
            return certificate.publicKey
        }

        fun getDevicePublicKeyInBase58(): String =
            Base58.base58Encode(
                BCECPublicKey(
                    getPublicKeyFromDeviceKey() as ECPublicKey,
                    BouncyCastleProvider.CONFIGURATION
                ).q.getEncoded(true)
            )

        private fun getCertificateFromKeystore(): Certificate {
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

        fun deleteDeviceKeyIfPresent() {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val haveKey = keyStore.containsAlias(KEY_ID)

            if (haveKey) {
                keyStore.deleteEntry(KEY_ID)
            }
        }

        fun deviceKeyExists(): Boolean {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            return keyStore.containsAlias(KEY_ID)
        }
    }

}

class ExternalDeviceKey(val publicKey: PublicKey) : VaultKey {

    override fun publicKeyUncompressed() =
        ECIESManager.extractUncompressedPublicKey(publicKey.encoded)

    override fun publicExternalRepresentation(): Base58EncodedPublicKey {
        return Base58EncodedDevicePublicKey(Base58.base58Encode(publicKeyUncompressed()))
    }

    override fun privateKeyRaw(): ByteArray {
        throw Exception("No Access to external device private key")
    }

    override fun encrypt(data: ByteArray) =
        ECIESManager.encryptMessage(
            dataToEncrypt = data,
            publicKeyBytes = publicKeyUncompressed()
        )

    override fun decrypt(data: ByteArray): ByteArray {
        throw Exception("Cannot decrypt with external device key")
    }

    override fun sign(data: ByteArray): ByteArray {
        throw Exception("Cannot sign data with external device key")
    }

    override fun verify(signedData: ByteArray, signature: ByteArray): Boolean {
        TODO("Not yet implemented")
    }
}