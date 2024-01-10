package co.censo.censo.presentation.import_phrase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ImportKeyUtil {
    fun recreateECPublicKey(rawBytes: ByteArray): PublicKey {
        val xAndYCoordinatesOnly = rawBytes.copyOfRange(1, rawBytes.size)
        val coordinateLength = xAndYCoordinatesOnly.size / 2
        val x = xAndYCoordinatesOnly.copyOfRange(0, coordinateLength)
        val y = xAndYCoordinatesOnly.copyOfRange(coordinateLength, xAndYCoordinatesOnly.size)

        val ecPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))

        val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val params =
            ECNamedCurveSpec("secp256r1", parameterSpec.curve, parameterSpec.g, parameterSpec.n)

        val publicKeySpec = ECPublicKeySpec(ecPoint, params)

        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(publicKeySpec)
    }

    fun convertRawSignatureToDerFormat(signature: ByteArray): ByteArray {
        fun isNegative(input: ByteArray) = input[0].toUByte() > 0x7f.toUByte()

        fun makePositive(input: ByteArray): ByteArray {
            return if (isNegative(input)) {
                byteArrayOf(0x00) + input
            } else {
                input
            }
        }

        val r = makePositive(signature.copyOfRange(0, 32))
        val s = makePositive(signature.copyOfRange(32, 64))

        val derSignature = ByteBuffer.allocate(2 + r.size + 2 + s.size + 2).apply {
            put(0x30)
            put((r.size + s.size + 4).toByte())
            put(0x02)
            put(r.size.toByte())
            put(r)
            put(0x02)
            put(s.size.toByte())
            put(s)
        }

        return derSignature.array()
    }

    fun isLinkExpired(timestamp: Long): ImportErrorType {
        val linkCreationTime = Instant.fromEpochMilliseconds(timestamp)
        val linkValidityStart = linkCreationTime.minus(10.seconds)
        val linkValidityEnd = linkCreationTime.plus(10.minutes)
        val now = Clock.System.now()

        return if (now > linkValidityEnd) {
            ImportErrorType.LINK_EXPIRED
        } else if (now < linkValidityStart) {
            ImportErrorType.LINK_IN_FUTURE
        } else {
            ImportErrorType.NONE
        }
    }
}