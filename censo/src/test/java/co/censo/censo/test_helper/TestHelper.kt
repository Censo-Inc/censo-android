package co.censo.censo.test_helper

import Base64EncodedData
import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.key.EncryptionKey
import java.security.SecureRandom
import java.util.Base64

fun createApproverKey(): EncryptionKey {
    val keyPair = ECHelper.createECKeyPair()
    return EncryptionKey(keyPair)
}

fun generateEntropy() = generateRandomBytes(64)

private fun generateRandomBytes(length: Int): Base64EncodedData {
    val bytes = ByteArray(length).also {
        SecureRandom().nextBytes(it)
    }
    return Base64EncodedData(Base64.getEncoder().encodeToString(bytes))
}