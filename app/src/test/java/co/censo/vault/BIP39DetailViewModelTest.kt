package co.censo.vault

import co.censo.vault.presentation.bip_39_detail.BIP39DetailViewModel
import co.censo.vault.storage.EncryptedBIP39
import co.censo.vault.storage.Storage
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.time.ZonedDateTime
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class BIP39DetailViewModelTest : BaseViewModelTest() {

    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var cryptographyManager: CryptographyManager

    private val dispatcher = StandardTestDispatcher()

    private lateinit var bip39DetailViewModel: BIP39DetailViewModel

    private val twentyFourWordBIP39Phrase =
        "market talent corn beef party situate domain guitar toast system tribe meat provide tennis believe coconut joy salon guide choose few obscure inflict horse"

    private val twelveWordBip39Phrase = "market talent corn beef party situate domain guitar toast system tribe meat"

    private val twentyFourWordDecryptedPhraseList =
        "WyJtYXJrZXQiLCJ0YWxlbnQiLCJjb3JuIiwiYmVlZiIsInBhcnR5Iiwic2l0dWF0ZSIsImRvbWFpbiIsImd1aXRhciIsInRvYXN0Iiwic3lzdGVtIiwidHJpYmUiLCJtZWF0IiwicHJvdmlkZSIsInRlbm5pcyIsImJlbGlldmUiLCJjb2NvbnV0Iiwiam95Iiwic2Fsb24iLCJndWlkZSIsImNob29zZSIsImZldyIsIm9ic2N1cmUiLCJpbmZsaWN0IiwiaG9yc2UiXQ=="

    private val twelveWordDecryptedPhraseList =
        "WyJtYXJrZXQiLCJ0YWxlbnQiLCJjb3JuIiwiYmVlZiIsInBhcnR5Iiwic2l0dWF0ZSIsImRvbWFpbiIsImd1aXRhciIsInRvYXN0Iiwic3lzdGVtIiwidHJpYmUiLCJtZWF0Il0="

    private val test1Bip39Name = "test1"

    private val test2Bip39Name = "test2"

    private val test1EncryptedBIP39 = "BKCv4TeFKsMzfc6eni1BI1qS2pKD0XcE7kezRIMXuKdvCJK4AmfLdTX4ha0lFLF9HoLjzHp2W6uBSA8OUUJfvx0cZrUpryU6ex1wKKiyw8/9rd4gHJydYy0NyovdvaWA/ieZU5qpDBj7vCsoChERVm/AM/j5xvbSQe5tjUiPDkzh3UcE70GmhPIw+6PVHjXMNGroqqgwWIGKoY7asK8m41+dQpuKzb6AU6u5HdvxJ5r5Z4Zeg6Xqdt6UllqdaR3OaBeVNwxL6N8pGju3bqXhvXoperI1iPm0tXIPSSUxxIbmfqWFy4hBfzS1hcIw9yN9xUq0t+z8xiHXJEGArSak6Npx+TwObZfK0jsG1+WQ55nsFxXJ5eST1ayZvuPaWA=="
    private val test2EncryptedBIP39 = "BJ35mILaLzxKV8/gOEumdr23dE8uv4+pdKfD6EKyvX6FLGhPL8D5UN43h7tS7uiEgjtSUcB7/d8AfcQxAHG3tLx8Aqu7ug6dT8l8D/wedQ5zNelp8uM8WPdz/CkxPQKHr3+gHzOpFncO9f2GSJ7sIRku5ipUzdxsm7xZWL+oZ+reP3Bgn92M1simJuQURIXsgsi16hY27wtFSJUlu1KJ2DKmAnS/aMQVMotehVhQdUDSnVrzInE="

    private val savedPhrases = mapOf(
        test1Bip39Name to EncryptedBIP39(
            base64 = test1EncryptedBIP39,
            createdAt = Clock.System.now()
        ),
        test2Bip39Name to EncryptedBIP39(
            base64 = test2EncryptedBIP39,
            createdAt = Clock.System.now()
        )
    )

    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(dispatcher)

        bip39DetailViewModel = BIP39DetailViewModel(
            storage = storage,
            cryptographyManager = cryptographyManager
        )
    }

    @Test
    fun `on start sets the name and triggers biometry`() = runTest {
        assert(bip39DetailViewModel.state.name.isEmpty())
        assert(bip39DetailViewModel.state.bioPromptTrigger is Resource.Uninitialized)

        bip39DetailViewModel.onStart(test1Bip39Name)

        assert(bip39DetailViewModel.state.name == test1Bip39Name)
        assert(bip39DetailViewModel.state.bioPromptTrigger is Resource.Success)
    }

    @Test
    fun `on biometry failure we set biometry state to error`() = runTest {
        bip39DetailViewModel.onStart(test1Bip39Name)

        bip39DetailViewModel.onBiometryFailed()

        assert(bip39DetailViewModel.state.name == test1Bip39Name)
        assert(bip39DetailViewModel.state.bioPromptTrigger is Resource.Error)
    }

    @Test
    fun `on biometry approval we retrieve the bip 39 phrase`() = runTest {
        whenever(storage.retrieveBIP39Phrases()).then { savedPhrases }
        whenever(
            cryptographyManager.decryptData(
                Base64.getDecoder().decode(test1EncryptedBIP39)
            )
        ).then {
            Base64.getDecoder().decode(twentyFourWordDecryptedPhraseList)
        }

        bip39DetailViewModel.onStart(test1Bip39Name)

        bip39DetailViewModel.onBiometryApproved()

        assert(bip39DetailViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assert(bip39DetailViewModel.state.bip39Phrase == twentyFourWordBIP39Phrase)
    }

    @Test
    fun `on biometry approval we can retrieve 12 word phrase`() = runTest {
        whenever(storage.retrieveBIP39Phrases()).then { savedPhrases }
        whenever(
            cryptographyManager.decryptData(
                Base64.getDecoder().decode(test2EncryptedBIP39)
            )
        ).then {
            Base64.getDecoder().decode(twelveWordDecryptedPhraseList)
        }

        bip39DetailViewModel.onStart(test2Bip39Name)

        bip39DetailViewModel.onBiometryApproved()

        assert(bip39DetailViewModel.state.bioPromptTrigger is Resource.Uninitialized)
        assert(bip39DetailViewModel.state.bip39Phrase == twelveWordBip39Phrase)
    }

    @Test
    fun `we can change index forward and backward when traversing a 24 word bip 39 phrase`() {
        whenever(storage.retrieveBIP39Phrases()).then { savedPhrases }
        whenever(
            cryptographyManager.decryptData(
                Base64.getDecoder().decode(test1EncryptedBIP39)
            )
        ).then {
            Base64.getDecoder().decode(twentyFourWordDecryptedPhraseList)
        }

        bip39DetailViewModel.onStart(test1Bip39Name)

        bip39DetailViewModel.onBiometryApproved()

        assert(bip39DetailViewModel.state.currentWordIndex == 0)
        assert(bip39DetailViewModel.state.lastWordIndex == 23)
        assert(bip39DetailViewModel.state.lastSetStartIndex == 20)

        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(true)

        assert(bip39DetailViewModel.state.currentWordIndex == 12)
        assert(bip39DetailViewModel.state.lastWordIndex == 23)
        assert(bip39DetailViewModel.state.lastSetStartIndex == 20)

        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(false)

        assert(bip39DetailViewModel.state.currentWordIndex == 8)
        assert(bip39DetailViewModel.state.lastWordIndex == 23)
        assert(bip39DetailViewModel.state.lastSetStartIndex == 20)

        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)

        assert(bip39DetailViewModel.state.currentWordIndex == 16)
        assert(bip39DetailViewModel.state.lastWordIndex == 23)
        assert(bip39DetailViewModel.state.lastSetStartIndex == 20)
    }

    @Test
    fun `we can change index forward and backward when traversing a 12 word bip 39 phrase`() {
        whenever(storage.retrieveBIP39Phrases()).then { savedPhrases }
        whenever(
            cryptographyManager.decryptData(
                Base64.getDecoder().decode(test2EncryptedBIP39)
            )
        ).then {
            Base64.getDecoder().decode(twelveWordDecryptedPhraseList)
        }

        bip39DetailViewModel.onStart(test2Bip39Name)

        bip39DetailViewModel.onBiometryApproved()

        assertEquals(bip39DetailViewModel.state.currentWordIndex, 0)
        assertEquals(bip39DetailViewModel.state.lastWordIndex, 11)
        assertEquals(bip39DetailViewModel.state.lastSetStartIndex, 8)

        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(true)

        assertEquals(bip39DetailViewModel.state.currentWordIndex, 0)
        assertEquals(bip39DetailViewModel.state.lastWordIndex, 11)
        assertEquals(bip39DetailViewModel.state.lastSetStartIndex, 8)

        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(true)
        bip39DetailViewModel.wordIndexChanged(false)

        assertEquals(bip39DetailViewModel.state.currentWordIndex, 8)
        assertEquals(bip39DetailViewModel.state.lastWordIndex, 11)
        assertEquals(bip39DetailViewModel.state.lastSetStartIndex, 8)

        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)
        bip39DetailViewModel.wordIndexChanged(false)

        assertEquals(bip39DetailViewModel.state.currentWordIndex, 4)
        assertEquals(bip39DetailViewModel.state.lastWordIndex, 11)
        assertEquals(bip39DetailViewModel.state.lastSetStartIndex, 8)
    }



    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}