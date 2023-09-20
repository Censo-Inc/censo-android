package co.censo.shared.data.cryptography.key

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import co.censo.shared.BuildConfig
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.networking.AuthInterceptor
import io.github.novacrypto.base58.Base58
import java.math.BigInteger
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey
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


/**
 *
 * This will represent a SECP256R1 key not stored in the Keystore or Secure Enclave
 *
 */
class EncryptionKey(val key: KeyPair) : VaultKey {

    override fun publicExternalRepresentation(): Base58EncodedPublicKey {
        return Base58EncodedDevicePublicKey(Base58.base58Encode(publicKeyUncompressed()))
    }

    override fun publicKeyUncompressed() =
        ECPublicKeyDecoder.extractUncompressedPublicKey(key.public.encoded)

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
            val privateKey = ECHelper.getPrivateKeyFromECBigIntAndCurve(raw) as ECPrivateKey
            val publicKey = ECPublicKeyDecoder.getPublicKeyFromPrivateKey(privateKey)

            return EncryptionKey(KeyPair(publicKey, privateKey))
        }

        fun generateRandomKey(): EncryptionKey =
            EncryptionKey(ECHelper.createECKeyPair())
    }

}

/**
 *
 * This will represent a SECP256R1 key stored on the users devices Keystore.
 *
 */
class InternalDeviceKey(private val keyId: String) : VaultKey {

    private val keystoreHelper = KeystoreHelper()

    override fun publicKeyUncompressed(): ByteArray {
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey(keyId)
        return ECPublicKeyDecoder.extractUncompressedPublicKey(publicKey.encoded)
    }

    override fun publicExternalRepresentation(): Base58EncodedPublicKey {
        return Base58EncodedDevicePublicKey(keystoreHelper.getDevicePublicKeyInBase58(keyId))
    }

    override fun privateKeyRaw(): ByteArray {
        throw Exception("Cannot access private key on Keystore Key")
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey(keyId)

        val compressedKey = ECPublicKeyDecoder.extractUncompressedPublicKey(publicKey.encoded)

        return ECIESManager.encryptMessage(
            dataToEncrypt = data,
            publicKeyBytes = compressedKey,
        )
    }

    override fun decrypt(data: ByteArray): ByteArray {
        return ECIESManager.decryptMessage(
            cipherData = data,
            privateKey = keystoreHelper.getOrCreateDeviceKey(keyId)
        )
    }

    override fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(KeystoreHelper.SHA_256_ECDSA)
        signature.initSign(keystoreHelper.getOrCreateDeviceKey(keyId))
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
        val publicKey = keystoreHelper.getPublicKeyFromDeviceKey(keyId)
        signatureKeystore.initVerify(publicKey)
        signatureKeystore.update(signedData)
        return signatureKeystore.verify(signature)
    }

    fun retrieveKey() = keystoreHelper.getOrCreateDeviceKey(keyId)
}

class KeystoreHelper {
    companion object {
        const val BIOMETRY_TIMEOUT = 15
        const val KEY_SIZE: Int = 256
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val SECP_256_R1 = "secp256r1"
        const val SHA_256_ECDSA = "SHA256withECDSA"
    }

    fun getOrCreateDeviceKey(keyId: String): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val key = keyStore.getKey(keyId, null)
        if (key != null) return key as PrivateKey

        return createECDeviceKey(keyId)
    }

    fun getPublicKeyFromDeviceKey(keyId: String): PublicKey {
        val certificate = getCertificateFromKeystore(keyId)
        return certificate.publicKey
    }

    fun getDevicePublicKeyInBase58(keyId: String): String =
        Base58.base58Encode(
            BCECPublicKey(
                getPublicKeyFromDeviceKey(keyId) as ECPublicKey,
                BouncyCastleProvider.CONFIGURATION
            ).q.getEncoded(true)
        )

    private fun getCertificateFromKeystore(keyId: String): Certificate {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        return keyStore.getCertificate(keyId)
    }

    private fun createECDeviceKey(keyId: String): PrivateKey {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val paramBuilder = KeyGenParameterSpec.Builder(
            keyId,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_AGREE_KEY
        )

        val parameterSpec = paramBuilder
            .setAlgorithmParameterSpec(ECGenParameterSpec(SECP_256_R1))
            .setKeySize(KEY_SIZE)
            .setIsStrongBoxBacked(BuildConfig.STRONGBOX_ENABLED)
            .setRandomizedEncryptionRequired(true)
            .setDigests(
                KeyProperties.DIGEST_SHA256
            )
            .build()

        kpg.initialize(parameterSpec)

        return kpg.generateKeyPair().private
    }

    fun deleteDeviceKeyIfPresent(keyId: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val haveKey = keyStore.containsAlias(keyId)

        if (haveKey) {
            keyStore.deleteEntry(keyId)
        }
    }

    fun deviceKeyExists(keyId: String): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(keyId)
    }
}

/**
 *
 * This will represent a SECP256R1 key stored on another devices Keystore or Secure Enclave
 *
 * We will never have access to this private key so it cannot decrypt or sign.
 *
 */

class ExternalDeviceKey(private val publicKey: PublicKey) : VaultKey {

    override fun publicKeyUncompressed() =
        ECPublicKeyDecoder.extractUncompressedPublicKey(publicKey.encoded)

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
        val signatureKeystore = Signature.getInstance(KeystoreHelper.SHA_256_ECDSA)
        signatureKeystore.initVerify(publicKey)
        signatureKeystore.update(signedData)
        return signatureKeystore.verify(signature)
    }
}