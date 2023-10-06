package co.censo.shared.data.cryptography

import io.github.novacrypto.base58.Base58
import org.bouncycastle.util.encoders.Base32
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random


fun generatePartitionId() : BigInteger {
    return BigInteger(generateRandom(64), 16)
}

fun generateBase32() : String {
    return Base32.toBase32String(
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
    for (i in 0..length) {
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

fun String.sha256(): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(this.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
}

fun String.toParticipantIdAsBigInteger(): BigInteger {
    val bytes = Base58.base58Decode(this)
    return BigInteger(
        1,
        when (bytes.size) {
            32 -> bytes
            64 -> bytes.slice(0..31).toByteArray()
            33, 65 -> bytes.slice(1..32).toByteArray()
            else -> throw Exception(":Invalid key")
        }
    )
}

fun String.toParticipantIdAsHexString() = toParticipantIdAsBigInteger().toByteArrayNoSign(32).toHexString().lowercase()

fun BigInteger.toByteArrayNoSign(): ByteArray {
    val byteArray = this.toByteArray()
    return if (byteArray[0].compareTo(0) == 0) {
        byteArray.slice(IntRange(1, byteArray.size - 1)).toByteArray()
    } else byteArray
}

fun ByteArray.toHexString(): String =
    joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }