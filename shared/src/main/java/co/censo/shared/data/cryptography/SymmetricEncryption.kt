package co.censo.shared.data.cryptography

import Base58EncodedPrivateKey
import Base64EncodedData
import co.censo.shared.data.cryptography.key.EncryptionKey
import io.github.novacrypto.base58.Base58
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.util.Arrays
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SymmetricEncryption {
    private val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(12).also {
            SecureRandom().nextBytes(it)
        }

        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(12 * Byte.SIZE_BITS, iv))
        return iv + cipher.doFinal(plaintext)
    }

    fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray {
        val iv = Arrays.copyOfRange(encryptedData, 0, 12)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(12 * Byte.SIZE_BITS, iv))

        return cipher.doFinal(Arrays.copyOfRange(encryptedData, 12, encryptedData.size))
    }
}

fun ByteArray.decryptFromByteArray(deviceKeyId: String) : Base58EncodedPrivateKey {
    val decryptedKey = SymmetricEncryption().decrypt(deviceKeyId.sha256digest(), this)
    return Base58EncodedPrivateKey(Base58.base58Encode(decryptedKey))
}

fun EncryptionKey.encryptToByteArray(deviceKeyId: String) : ByteArray {
    return SymmetricEncryption().encrypt(
        deviceKeyId.sha256digest(),
        Base58EncodedPrivateKey(Base58.base58Encode(this.privateKeyRaw())).bigInt()
            .toByteArrayNoSign()
    )
}

fun ByteArray.decryptWithEntropy(deviceKeyId: String, entropy: Base64EncodedData) : Base58EncodedPrivateKey {
    val digest = SHA3.Digest256()

    digest.update(deviceKeyId.toByteArray())
    digest.update(entropy.bytes)

    val decryptedKey = SymmetricEncryption().decrypt(digest.digest(), this)
    return Base58EncodedPrivateKey(Base58.base58Encode(decryptedKey))
}

fun EncryptionKey.encryptWithEntropy(deviceKeyId: String, entropy: Base64EncodedData) : ByteArray {
    val digest = SHA3.Digest256()

    digest.update(deviceKeyId.toByteArray())
    digest.update(entropy.bytes)

    val keyBytes =
        Base58EncodedPrivateKey(Base58.base58Encode(privateKeyRaw())).bigInt().toByteArrayNoSign()

    return SymmetricEncryption().encrypt(
        digest.digest(),
        keyBytes
    )
}