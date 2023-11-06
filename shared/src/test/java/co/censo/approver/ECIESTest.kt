package co.censo.approver

import co.censo.shared.data.cryptography.ECHelper
import co.censo.shared.data.cryptography.ECIESManager
import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import org.junit.Test
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals

class ECIESTest {
    @Test
    fun encryptAndDecryptFlow() {
        val keyPair = ECHelper.createECKeyPair()
        val publicKey = ECPublicKeyDecoder.extractUncompressedPublicKey(keyPair.public.encoded)

        val plainTextValues = listOf(
            "Whatsupppppppppp",
            "a decently long one that runs on for quite a long time and let us see about this and that over there, and this is getting more drawn out and then we are seeing what to do around here also long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time and let us long one that runs on for quite a long time ",
            "jkghf4536789guihjvbn  @$%#^$&%*^",
            "097865432678,hmgfdshjKJHGFDSFHJjkhgfjvbmn!@#$%^&*KLJHGFDCVBNMJHMN"
        )

        for (plainText in plainTextValues) {
            val encryptedData = ECIESManager.encryptMessage(
                dataToEncrypt = plainText.toByteArray(Charsets.UTF_8),
                publicKeyBytes = publicKey
            )

            val decryptedData = ECIESManager.decryptMessage(
                cipherData = encryptedData,
                privateKey = keyPair.private
            )

            assertEquals(plainText, String(decryptedData, Charsets.UTF_8))
        }
    }

    @Test(expected = AEADBadTagException::class)
    fun cannotDecryptWithWrongKey() {
        val keyPair = ECHelper.createECKeyPair()
        val publicKey = ECPublicKeyDecoder.extractUncompressedPublicKey(keyPair.public.encoded)

        val otherKeyPair = ECHelper.createECKeyPair()

        val plainText = "Whatsupppppppppp"

        val encryptedData = ECIESManager.encryptMessage(
            dataToEncrypt = plainText.toByteArray(Charsets.UTF_8),
            publicKeyBytes = publicKey
        )

        ECIESManager.decryptMessage(
            cipherData = encryptedData,
            privateKey = otherKeyPair.private
        )
    }
}