package co.censo.shared.data.model

import Base58EncodedPublicKey

data class InitialKeyData(
    val encryptedPrivateKey: ByteArray,
    val publicKey: Base58EncodedPublicKey
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InitialKeyData

        if (!encryptedPrivateKey.contentEquals(other.encryptedPrivateKey)) return false
        if (publicKey != other.publicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedPrivateKey.contentHashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }
}