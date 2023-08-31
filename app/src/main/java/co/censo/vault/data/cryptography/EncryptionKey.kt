package co.censo.vault.data.cryptography

import io.github.novacrypto.base58.Base58
import java.math.BigInteger
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey

class EncryptionKey(val key: KeyPair) {

    fun publicExternalRepresentation(): String {
        return Base58.base58Encode(publicKeyUncompressed())
    }

    fun publicKeyUncompressed() = ECIESManager.extractUncompressedPublicKey(key.public.encoded)

    fun privateKeyRaw(): BigInteger =
        (key.private as ECPrivateKey).s

    fun encrypt(data: ByteArray): ByteArray =
        ECIESManager.encryptMessage(data, publicKeyUncompressed())

    fun decrypt(data: ByteArray) : ByteArray =
        ECIESManager.decryptMessage(data, key.private)

    companion object {
        fun generateFromPrivateKeyRaw(raw: BigInteger) : EncryptionKey {
            val privateKey = ECIESManager.getPrivateKeyFromECBigIntAndCurve(raw) as ECPrivateKey
            val publicKey = ECIESManager.getPublicKeyFromPrivateKey(privateKey)

            return EncryptionKey(KeyPair(publicKey, privateKey))
        }

        fun generateRandomKey() : EncryptionKey =
            EncryptionKey(ECIESManager.createSecp256R1KeyPair())
    }

}