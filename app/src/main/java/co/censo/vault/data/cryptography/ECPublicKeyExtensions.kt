import android.os.Build
import co.censo.vault.data.cryptography.ECIESManager
import io.github.novacrypto.base58.Base58
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.HexFormat

private val bcProvider = BouncyCastleProvider()
private const val curveName = "secp256r1"

object ECPublicKeyDecoder {
    init {
        java.security.Security.addProvider(bcProvider)
    }

    fun fromHexEncodedString(hexKey: String): ECPublicKey {
        // create a public key using the provided hex string
        val bytes = Hex.decode(hexKey)
        val keyLength = 64
        val startingOffset = if (bytes.size == keyLength + 1 && bytes[0].compareTo(4) == 0) 1 else 0
        val x = bytes.slice(IntRange(startingOffset, 31 + startingOffset)).toByteArray()
        val y = bytes.slice(IntRange(startingOffset + 32, 63 + startingOffset)).toByteArray()

        val pubPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))
        val params = AlgorithmParameters.getInstance("EC", bcProvider).apply {
            init(ECGenParameterSpec(curveName))
        }
        val pubECSpec = ECPublicKeySpec(
            pubPoint,
            params.getParameterSpec(ECParameterSpec::class.java),
        )
        return KeyFactory.getInstance("EC", bcProvider)
            .generatePublic(pubECSpec) as ECPublicKey
    }

    fun fromBase58EncodedString(base58Key: String): ECPublicKey {
        if (Build.VERSION.SDK_INT >= 34) {
            val hexFormatter = HexFormat.of()
            val keyBytes = Base58.base58Decode(base58Key)
            return if (keyBytes.size == 33 || keyBytes.size == 32) {
                // compressed bytes case
                val hexKey = hexFormatter.formatHex(keyBytes)
                val spec = ECNamedCurveTable.getParameterSpec(curveName)
                val pubPoint = spec.curve.decodePoint(Hex.decode(hexKey))
                fromHexEncodedString(hexFormatter.formatHex(pubPoint.getEncoded(false)))
            } else {
                fromHexEncodedString(hexFormatter.formatHex(keyBytes))
            }
        } else {
            val keyBytes = Base58.base58Decode(base58Key)
            return ECIESManager.getPublicKeyFromBytes(keyBytes)
        }
    }
}