package co.censo.approver

import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.SecretSharer
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.security.interfaces.ECPrivateKey
import kotlin.random.Random

class KeysAndShardsTest {

    @Test
    fun `encrypt and decrypt master private key`() {
        val masterKeyPair = ECHelper.createECKeyPair()
        val policyKeyPair = ECHelper.createECKeyPair()

        val masterPrivateKey = String(masterKeyPair.private.encoded, Charsets.UTF_8)

        val policyPublicKey =
            ECPublicKeyDecoder.extractUncompressedPublicKey(policyKeyPair.public.encoded)

        val encryptedMasterKey = ECIESManager.encryptMessage(
            dataToEncrypt = masterPrivateKey.toByteArray(Charsets.UTF_8),
            publicKeyBytes = policyPublicKey
        )

        val decryptedMasterKey = ECIESManager.decryptMessage(
            cipherData = encryptedMasterKey,
            privateKey = policyKeyPair.private
        )

        assertEquals(masterPrivateKey, String(decryptedMasterKey, Charsets.UTF_8))
    }

    @Test
    fun `shard and recreate private key`() {
        val policyKeyPair = ECHelper.createECKeyPair()

        val secret = (policyKeyPair.private as ECPrivateKey).s

        val secretSharer = SecretSharer(
            secret = secret,
            threshold = 3,
            participants = (1..6).map { BigInteger.valueOf(Random.nextLong()) }
        )

        Assert.assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[0], secretSharer.shards[1], secretSharer.shards[2]
                )
            )
        )

        Assert.assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[2], secretSharer.shards[4], secretSharer.shards[5]
                )
            )
        )

        Assert.assertNotEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[2],
                    secretSharer.shards[4],
                )
            )
        )
    }

    @Test
    fun `decrypt data from recovered secret`() {
        val policyKeyPair = ECHelper.createECKeyPair()
        val policyPublicKey =
            ECPublicKeyDecoder.extractUncompressedPublicKey(policyKeyPair.public.encoded)

        val textToEncrypt = "some text here"

        val encryptedData =
            ECIESManager.encryptMessage(textToEncrypt.toByteArray(Charsets.UTF_8), policyPublicKey)

        val privateKeyRaw = (policyKeyPair.private as ECPrivateKey).s

        val secretSharer = SecretSharer(
            secret = privateKeyRaw,
            threshold = 3,
            participants = (1..6).map { BigInteger.valueOf(Random.nextLong()) }
        )

        val recoveredSecret = secretSharer.recoverSecret(
            listOf(
                secretSharer.shards[0], secretSharer.shards[1], secretSharer.shards[2]
            )
        )

        val recoveredPrivateKey = ECHelper.getPrivateKeyFromECBigIntAndCurve(recoveredSecret)

        val decryptedData = ECIESManager.decryptMessage(
            cipherData = encryptedData,
            privateKey = recoveredPrivateKey
        )

        assertEquals(textToEncrypt, String(decryptedData, Charsets.UTF_8))
    }

    @Test
    fun `testing public key`() {
        val policyKeyPair = ECHelper.createECKeyPair()
        val fromPrivateKey = ECPublicKeyDecoder.getPublicKeyFromPrivateKey(policyKeyPair.private as ECPrivateKey)

        assertEquals(fromPrivateKey, policyKeyPair.public)
    }
}