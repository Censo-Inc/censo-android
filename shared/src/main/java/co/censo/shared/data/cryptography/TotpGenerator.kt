package co.censo.shared.data.cryptography

import co.censo.shared.data.cryptography.TotpGenerator.Companion.CODE_LENGTH
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow

interface TotpGenerator {
    companion object {
        const val CODE_LENGTH = 6
        const val CODE_EXPIRATION = 60L
    }

    fun generateSecret() : String
    fun generateCode(secret: String, counter: Long) : String
}

object TotpGeneratorImpl : TotpGenerator {
    private val random = SecureRandom()

    override fun generateSecret(): String {
        val secretBytes = ByteArray(10)
        random.nextBytes(secretBytes)
        return Base32().encodeAsString(secretBytes)
    }

    override fun generateCode(secret: String, counter: Long): String {
        // convert counter to long and insert into bytearray
        val payload: ByteArray = ByteBuffer.allocate(8).putLong(0, counter).array()
        val hash = generateHash(secret, payload)
        val truncatedHash = truncateHash(hash)
        // generate code by computing the hash as integer mod 1000000
        val code = ByteBuffer.wrap(truncatedHash).int % 10.0.pow(CODE_LENGTH).toInt()
        // pad code to correct length, could be too small
        return code.toString().padStart(CODE_LENGTH, '0')
    }

    private fun generateHash(secret: String, payload: ByteArray): ByteArray {
        val key = Base32().decode(secret)
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "RAW"))
        return mac.doFinal(payload)
    }

    private fun truncateHash(hash: ByteArray): ByteArray {
        // last nibble of hash
        val offset = hash.last().and(0x0F).toInt()
        // get 4 bytes of the hash starting at the offset
        val truncatedHash = ByteArray(4)
        for (i in 0..3) {
            truncatedHash[i] = hash[offset + i]
        }
        // remove most significant bit
        truncatedHash[0] = truncatedHash[0].and(0x7F)
        return truncatedHash
    }
}