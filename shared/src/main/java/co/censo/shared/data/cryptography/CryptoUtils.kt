package co.censo.shared.data.cryptography

import Base64EncodedData
import io.github.novacrypto.base58.Base58
import org.apache.commons.codec.binary.Base32
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow
import kotlin.random.Random

fun String.generateVerificationCodeSignData(timeMillis: Long) =
    this.toByteArray() + timeMillis.toString().toByteArray()


fun generatePartitionId() : BigInteger {
    return BigInteger(generateRandom(64), 16)
}

fun generateBase32() : String {
    return Base32().encodeAsString(
        generateRandom(
            letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
            length = 10
        ).toByteArray(Charsets.UTF_8)
    )
}

fun generateBase64() : String {
    return Base64.toBase64String(
        generateRandom(
            letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
            length = 10
        ).toByteArray(Charsets.UTF_8)
    )
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

fun generateHexString(length: Int = 64): String {
    val alphaChars = ('0'..'9').toList().toTypedArray() + ('a'..'f').toList().toTypedArray() + ('A'..'F').toList().toTypedArray()
    return (1..length).map { alphaChars.random().toChar() }.toMutableList().joinToString("")
}

fun BigInteger.toHexString(): String {
    return this.toByteArrayNoSign().toHexString().lowercase()
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