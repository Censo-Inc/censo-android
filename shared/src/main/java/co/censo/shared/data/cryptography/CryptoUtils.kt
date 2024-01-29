package co.censo.shared.data.cryptography

import Base64EncodedData
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.util.DigestFactory
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

fun String.generateVerificationCodeSignData(timeMillis: Long) =
    this.toByteArray() + timeMillis.toString().toByteArray()


fun generatePartitionId() : BigInteger {
    return BigInteger(generateRandom(64), 16)
}

fun generateRandom(length: Int, letters: String = "ABCDEF0123456789",) : String {
    val len = letters.count()
    var partitionId = ""
    for (i in 1..length) {
        val randomIndex = Random.nextInt(until = len)
        val randomCharacter = letters[randomIndex]
        partitionId += randomCharacter
    }
    return partitionId
}

fun ByteArray.toPaddedHexString(length: Int) = joinToString("") { "%02X".format(it) }.padStart(length, '0')
fun ByteArray.pad(length: Int): ByteArray = Hex.decode(this.toPaddedHexString(length * 2))
fun BigInteger.toByteArrayNoSign(len: Int): ByteArray {
    val byteArray = this.toByteArray()
    return when {
        byteArray.size == len + 1 && byteArray[0].compareTo(0) == 0 -> byteArray.slice(IntRange(1, byteArray.size - 1)).toByteArray()
        byteArray.size < len -> byteArray.pad(len)
        else -> byteArray
    }
}

fun String.hexStringToByteArray(): ByteArray {
    return this.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun ByteArray.sha256digest(): ByteArray {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(this)
}

fun String.sha256digest() = this.toByteArray().sha256digest()
fun String.sha256() = this.sha256digest().toHexString()
fun ByteArray.sha256Base64() = this.sha256digest().base64Encoded()
fun ByteArray.sha256() = this.sha256digest().toHexString()

fun BigInteger.toByteArrayNoSign(): ByteArray {
    val byteArray = this.toByteArray()
    return if (byteArray[0].compareTo(0) == 0) {
        byteArray.slice(IntRange(1, byteArray.size - 1)).toByteArray()
    } else byteArray
}

fun ByteArray.toHexString(): String =
    joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun ByteArray.base64Encoded(): Base64EncodedData =
    Base64EncodedData(java.util.Base64.getEncoder().encodeToString(this))

fun ByteArray.toBinaryString(length: Int): String = BigInteger(1, this).toString(2).padStart(length, '0')

fun String.pbkdf2WithHmacSHA224(salt: ByteArray? = null, iterationCount: Int = 120_000, keyLength: Int = 32): ByteArray {
    val generator = PKCS5S2ParametersGenerator(DigestFactory.createSHA224())
    generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(this.toCharArray()), salt, iterationCount)
    val keyParams = generator.generateDerivedMacParameters(keyLength * 8) as KeyParameter
    return keyParams.key
}
