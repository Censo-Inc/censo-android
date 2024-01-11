package co.censo.shared.data.cryptography

import android.os.Build
import co.censo.shared.data.cryptography.ECHelper.EC
import co.censo.shared.data.cryptography.key.KeystoreHelper.Companion.SECP_256_R1
import io.github.novacrypto.base58.Base58
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERBitString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldF2m
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.EllipticCurve
import java.util.HexFormat
import java.nio.ByteBuffer
import java.security.PublicKey

private val bcProvider = BouncyCastleProvider()

object ECHelper {

    private const val SECP_256_R1 = "secp256r1"
    const val ECDH = "ECDH"
    const val EC = "EC"

    val keyFactory = KeyFactory.getInstance(EC, BouncyCastleProvider())
    val keyPairGenerator = KeyPairGenerator.getInstance(EC, BouncyCastleProvider())

    val spec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(SECP_256_R1)
    val params: ECParameterSpec = ECNamedCurveSpec(SECP_256_R1, spec.curve, spec.g, spec.n)
    val bouncyParams: org.bouncycastle.jce.spec.ECParameterSpec =
        ECNamedCurveParameterSpec(SECP_256_R1, spec.curve, spec.g, spec.n)

    init {
        val secp256r1 = ECNamedCurveTable.getParameterSpec(SECP_256_R1)
        keyPairGenerator.initialize(secp256r1)
    }

    fun getPrivateKeyFromECBigIntAndCurve(s: BigInteger): PrivateKey {
        val privateKeySpec = ECPrivateKeySpec(s, spec)
        return keyFactory.generatePrivate(privateKeySpec)
    }

    fun createECKeyPair(): KeyPair {
        return keyPairGenerator.generateKeyPair()
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
}

object ECPublicKeyDecoder {
    init {
        java.security.Security.addProvider(bcProvider)
    }

    fun extractUncompressedPublicKey(uncompressedPublicKey: ByteArray): ByteArray {
        val sequence: ASN1Sequence = DERSequence.getInstance(uncompressedPublicKey)
        val subjectPublicKey: DERBitString = sequence.getObjectAt(1) as DERBitString
        return subjectPublicKey.bytes
    }

    fun getPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey {
        val q: org.bouncycastle.math.ec.ECPoint = ECHelper.spec.g.multiply(privateKey.s)
        val pubSpec = org.bouncycastle.jce.spec.ECPublicKeySpec(q, ECHelper.spec)
        return ECHelper.keyFactory.generatePublic(pubSpec) as ECPublicKey
    }

    fun getPublicKeyFromBytes(pubKey: ByteArray): ECPublicKey {
        val securityPoint: org.bouncycastle.math.ec.ECPoint =
            createPoint(ECHelper.params.curve, pubKey)
        val pubKeySpec =
            org.bouncycastle.jce.spec.ECPublicKeySpec(securityPoint, ECHelper.bouncyParams)

        return ECHelper.keyFactory.generatePublic(pubKeySpec) as ECPublicKey
    }

    private fun createPoint(
        curve: EllipticCurve,
        encoded: ByteArray?
    ): org.bouncycastle.math.ec.ECPoint {
        val c: ECCurve = if (curve.field is ECFieldFp) {
            ECCurve.Fp(
                (curve.field as ECFieldFp).p, curve.a, curve.b, null, null
            )
        } else {
            val k = (curve.field as ECFieldF2m).midTermsOfReductionPolynomial
            if (k.size == 3) {
                ECCurve.F2m(
                    (curve.field as ECFieldF2m).m,
                    k[2], k[1], k[0], curve.a, curve.b, null, null
                )
            } else {
                ECCurve.F2m(
                    (curve.field as ECFieldF2m).m, k[0], curve.a, curve.b, null, null
                )
            }
        }
        return c.decodePoint(encoded)
    }


    private fun fromHexEncodedString(hexKey: String): ECPublicKey {
        // create a public key using the provided hex string
        val bytes = Hex.decode(hexKey)
        val keyLength = 64
        val startingOffset = if (bytes.size == keyLength + 1 && bytes[0].compareTo(4) == 0) 1 else 0
        val x = bytes.slice(IntRange(startingOffset, 31 + startingOffset)).toByteArray()
        val y = bytes.slice(IntRange(startingOffset + 32, 63 + startingOffset)).toByteArray()

        val pubPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))
        val params = AlgorithmParameters.getInstance(EC, bcProvider).apply {
            init(ECGenParameterSpec(SECP_256_R1))
        }
        val pubECSpec = ECPublicKeySpec(
            pubPoint,
            params.getParameterSpec(ECParameterSpec::class.java),
        )
        return KeyFactory.getInstance(EC, bcProvider)
            .generatePublic(pubECSpec) as ECPublicKey
    }

    fun fromBase58EncodedString(base58Key: String): ECPublicKey {
        if (Build.VERSION.SDK_INT >= 34) {
            val hexFormatter = HexFormat.of()
            val keyBytes = Base58.base58Decode(base58Key)
            return if (keyBytes.size == 33 || keyBytes.size == 32) {
                // compressed bytes case
                val hexKey = hexFormatter.formatHex(keyBytes)
                val spec = ECNamedCurveTable.getParameterSpec(SECP_256_R1)
                val pubPoint = spec.curve.decodePoint(Hex.decode(hexKey))
                fromHexEncodedString(hexFormatter.formatHex(pubPoint.getEncoded(false)))
            } else {
                fromHexEncodedString(hexFormatter.formatHex(keyBytes))
            }
        } else {
            val keyBytes = Base58.base58Decode(base58Key)
            return getPublicKeyFromBytes(keyBytes)
        }
    }

    fun recreateECPublicKey(rawBytes: ByteArray): PublicKey {
        val xAndYCoordinatesOnly = rawBytes.copyOfRange(1, rawBytes.size)
        val coordinateLength = xAndYCoordinatesOnly.size / 2
        val x = xAndYCoordinatesOnly.copyOfRange(0, coordinateLength)
        val y = xAndYCoordinatesOnly.copyOfRange(coordinateLength, xAndYCoordinatesOnly.size)

        val ecPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))

        val parameterSpec = ECNamedCurveTable.getParameterSpec(SECP_256_R1)
        val params =
            ECNamedCurveSpec(SECP_256_R1, parameterSpec.curve, parameterSpec.g, parameterSpec.n)

        val publicKeySpec = ECPublicKeySpec(ecPoint, params)

        val keyFactory = KeyFactory.getInstance(EC)
        return keyFactory.generatePublic(publicKeySpec)
    }
}
