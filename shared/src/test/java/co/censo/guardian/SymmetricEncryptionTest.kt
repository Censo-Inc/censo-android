package co.censo.guardian

import co.censo.shared.data.cryptography.SymmetricEncryption
import co.censo.shared.data.cryptography.sha256digest
import junit.framework.TestCase.assertEquals
import org.junit.Test

class SymmetricEncryptionTest {

    @Test
    fun `test symmetric encryption`() {
        val symmetricEncryption = SymmetricEncryption()
        val message = "message"
        val key = "user-identifier".sha256digest()
        val encrypted = symmetricEncryption.encrypt(
            key,
            message.encodeToByteArray(),
        )
        assertEquals(
            message,
            String(symmetricEncryption.decrypt(
                key,
                encrypted
            ))
        )
    }
}