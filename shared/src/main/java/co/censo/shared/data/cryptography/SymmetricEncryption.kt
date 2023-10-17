package co.censo.shared.data.cryptography

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.Arrays
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