package co.censo.vault

import co.censo.vault.presentation.add_bip39.AddBIP39ViewModel
import co.censo.vault.storage.BIP39Phrases
import co.censo.vault.storage.EncryptedBIP39
import co.censo.vault.storage.Storage
import com.nhaarman.mockitokotlin2.capture
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import java.time.ZonedDateTime


@OptIn(ExperimentalCoroutinesApi::class)
class AddBIP39ViewModelTest : BaseViewModelTest() {

    @Captor
    lateinit var captor: ArgumentCaptor<BIP39Phrases>

    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var cryptographyManager: CryptographyManager

    private val dispatcher = StandardTestDispatcher()

    private lateinit var addBIP39ViewModel: AddBIP39ViewModel

    private val validBIP39Phrase = "market talent corn beef party situate domain guitar toast system tribe meat provide tennis believe coconut joy salon guide choose few obscure inflict horse"

    private val invalidBIP39Phrase = "train talent corn weight party situate domain guitar toast system tribe meat provide tennis believe coconut joy salon guide choose few obscure inflict horse"

    private val shortBIP39Phrase = "legend light country fruit future noble inspire sound hospital gun welcome bulb carbon apart"

    private val longBIP39Phrase = "market talent corn beef party situate domain guitar toast system tribe meat provide tennis believe coconut joy salon guide choose few obscure inflict horse light country fruit future noble inspire sound hospital gun welcome bulb carbon apart"

    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(dispatcher)

        addBIP39ViewModel = AddBIP39ViewModel(
            storage = storage,
            cryptographyManager = cryptographyManager
        )
    }

    @Test
    fun `user can update their name`() {
        assert(addBIP39ViewModel.state.name.isEmpty())

        addBIP39ViewModel.updateName(" uPdatEd nAme")

        assertEquals(addBIP39ViewModel.state.name, "updated name")

        addBIP39ViewModel.updateName("    updated name    ")

        assertEquals(addBIP39ViewModel.state.name, "updated name")
    }

    @Test
    fun `user can update their phrase`() {
        assert(addBIP39ViewModel.state.userEnteredPhrase.isEmpty())

        addBIP39ViewModel.updateUserEnteredPhrase(validBIP39Phrase)

        assert(addBIP39ViewModel.state.userEnteredPhrase == validBIP39Phrase)
    }

    @Test
    fun `do not validate phrase on entry`() {
        assert(addBIP39ViewModel.state.userEnteredPhrase.isEmpty())

        addBIP39ViewModel.updateUserEnteredPhrase(invalidBIP39Phrase)

        assert(addBIP39ViewModel.state.userEnteredPhrase == invalidBIP39Phrase)
        assert(addBIP39ViewModel.state.userEnteredPhraseError == null)
    }

    @Test
    fun `cannot submit if either name or phrase is empty`() {
        assert(!addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateName("name")

        assert(!addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateUserEnteredPhrase("phrase")

        assert(addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateName("")

        assert(!addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateName("okok")

        assert(addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateUserEnteredPhrase("")

        assert(!addBIP39ViewModel.canSubmit())

        addBIP39ViewModel.updateUserEnteredPhrase("phrase")

        assert(addBIP39ViewModel.canSubmit())
    }

    @Test
    fun `invalid phrase triggers phrase and submit error`() {
        addBIP39ViewModel.updateName("name")
        addBIP39ViewModel.updateUserEnteredPhrase(invalidBIP39Phrase)

        addBIP39ViewModel.submit()

        assert(!addBIP39ViewModel.state.userEnteredPhraseError.isNullOrEmpty())
        assert(addBIP39ViewModel.state.submitStatus is Resource.Error)
    }

    @Test
    fun `short phrase triggers phrase and submit error`() {
        addBIP39ViewModel.updateName("name")
        addBIP39ViewModel.updateUserEnteredPhrase(shortBIP39Phrase)

        addBIP39ViewModel.submit()

        assert(!addBIP39ViewModel.state.userEnteredPhraseError.isNullOrEmpty())
        assert(addBIP39ViewModel.state.submitStatus is Resource.Error)
    }

    @Test
    fun `long phrase triggers phrase and submit error`() {
        addBIP39ViewModel.updateName("name")
        addBIP39ViewModel.updateUserEnteredPhrase(longBIP39Phrase)

        addBIP39ViewModel.submit()

        assert(!addBIP39ViewModel.state.userEnteredPhraseError.isNullOrEmpty())
        assert(addBIP39ViewModel.state.submitStatus is Resource.Error)
    }

    @Test
    fun `no name triggers name and submit error`() {
        addBIP39ViewModel.updateName("")
        addBIP39ViewModel.updateUserEnteredPhrase(invalidBIP39Phrase)

        addBIP39ViewModel.submit()

        assert(!addBIP39ViewModel.state.nameError.isNullOrEmpty())
        assert(addBIP39ViewModel.state.submitStatus is Resource.Error)
    }

    @Test
    fun `can add a single phrase to empty phrase map`() {
        val dataToEncrypt =
            jsonMapper.writeValueAsString(
                PhraseValidator.format(validBIP39Phrase).split(" ")
            )

        whenever(storage.retrieveBIP39Phrases()).then { emptyMap<String, EncryptedBIP39>() }
        whenever(cryptographyManager.encryptData(dataToEncrypt)).then {
            validBIP39Phrase.encodeToByteArray()
        }

        addBIP39ViewModel.updateName("test1")
        addBIP39ViewModel.updateUserEnteredPhrase(validBIP39Phrase)

        addBIP39ViewModel.submit()

        verify(cryptographyManager).encryptData(dataToEncrypt)

        verify(storage).saveBIP39Phrases(capture(captor))

        assert(captor.value.containsKey("test1"))
        assert(captor.value.size == 1)

        assert(addBIP39ViewModel.state.submitStatus is Resource.Success)
    }

    @Test
    fun `can add a phrase to existing phrase map`() {
        val dataToEncrypt =
            jsonMapper.writeValueAsString(
                PhraseValidator.format(validBIP39Phrase).split(" ")
            )

        whenever(storage.retrieveBIP39Phrases()).then {
            mapOf(
                "test2" to EncryptedBIP39(
                    "",
                    ZonedDateTime.now()
                )
            )
        }
        whenever(cryptographyManager.encryptData(dataToEncrypt)).then {
            validBIP39Phrase.encodeToByteArray()
        }

        addBIP39ViewModel.updateName("test1")
        addBIP39ViewModel.updateUserEnteredPhrase(validBIP39Phrase)

        addBIP39ViewModel.submit()

        verify(cryptographyManager).encryptData(dataToEncrypt)

        verify(storage).saveBIP39Phrases(capture(captor))

        assert(captor.value.containsKey("test1"))
        assert(captor.value.containsKey("test2"))
        assert(captor.value.size == 2)

        assert(addBIP39ViewModel.state.submitStatus is Resource.Success)
    }

    @Test
    fun `cannot add a same named phrase`() {
        val dataToEncrypt =
            jsonMapper.writeValueAsString(
                PhraseValidator.format(validBIP39Phrase).split(" ")
            )

        whenever(storage.retrieveBIP39Phrases()).then {
            mapOf(
                "test1" to EncryptedBIP39(
                    "",
                    ZonedDateTime.now()
                )
            )
        }
        whenever(cryptographyManager.encryptData(dataToEncrypt)).then {
            validBIP39Phrase.encodeToByteArray()
        }

        addBIP39ViewModel.updateName("test1")
        addBIP39ViewModel.updateUserEnteredPhrase(validBIP39Phrase)

        addBIP39ViewModel.submit()

        assert(addBIP39ViewModel.state.nameError == "You already stored BIP39 phrase with this name")
        assert(addBIP39ViewModel.state.submitStatus is Resource.Error)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}