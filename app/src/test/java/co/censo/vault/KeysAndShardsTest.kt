package co.censo.vault

import co.censo.vault.data.cryptography.CryptographyManagerImpl
import co.censo.vault.data.cryptography.ECIESManager
import co.censo.vault.data.cryptography.SecretSharer
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.security.interfaces.ECPrivateKey
import kotlin.random.Random

class KeysAndShardsTest {

    @Test
    fun `encrypt and decrypt master private key`() {
        val masterKeyPair = ECIESManager.createSecp256R1KeyPair()
        val policyKeyPair = ECIESManager.createSecp256R1KeyPair()

        val masterPrivateKey = String(masterKeyPair.private.encoded, Charsets.UTF_8)

        val policyPublicKey =
            ECIESManager.extractUncompressedPublicKey(policyKeyPair.public.encoded)

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
        val policyKeyPair = ECIESManager.createSecp256R1KeyPair()

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
        val policyKeyPair = ECIESManager.createSecp256R1KeyPair()
        val policyPublicKey =
            ECIESManager.extractUncompressedPublicKey(policyKeyPair.public.encoded)

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

        val recoveredPrivateKey = ECIESManager.getPrivateKeyFromECBigIntAndCurve(recoveredSecret)

        val decryptedData = ECIESManager.decryptMessage(
            cipherData = encryptedData,
            privateKey = recoveredPrivateKey
        )

        assertEquals(textToEncrypt, String(decryptedData, Charsets.UTF_8))
    }

    @Test
    fun `testing public key`() {
        val policyKeyPair = ECIESManager.createSecp256R1KeyPair()
        val fromPrivateKey = ECIESManager.getPublicKeyFromPrivateKey(policyKeyPair.private as ECPrivateKey)

        assertEquals(fromPrivateKey, policyKeyPair.public)
    }
}