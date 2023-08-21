package co.censo.vault

import co.censo.vault.presentation.home.HomeViewModel
import co.censo.vault.data.storage.BIP39Phrases
import co.censo.vault.data.storage.EncryptedBIP39
import co.censo.vault.data.storage.Storage
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class HomeViewModelTest : BaseViewModelTest() {

    @Mock
    lateinit var storage: Storage

    private val dispatcher = StandardTestDispatcher()

    private lateinit var homeViewModel: HomeViewModel

    private val emptyPhrases: BIP39Phrases = emptyMap()

    private val examplePhrases: BIP39Phrases = mapOf(
        "test1" to EncryptedBIP39(
            base64 = Base64.getEncoder().encodeToString("this is  a test string".toByteArray()),
            createdAt = Clock.System.now()
        )
    )

    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(dispatcher)

        homeViewModel = HomeViewModel(storage)
    }

    @Test
    fun `on start attempts to get bip 39 phrases`() = runTest {

        whenever(storage.retrieveBIP39Phrases()).then { examplePhrases }
        assert(homeViewModel.state.phrases.isEmpty())

        homeViewModel.onStart()
        advanceUntilIdle()

        verify(storage, times(1)).retrieveBIP39Phrases()

        assert(homeViewModel.state.phrases == examplePhrases)
    }

    @Test
    fun `retrieving empty phrases sets state to empty phrases`() = runTest {

        whenever(storage.retrieveBIP39Phrases()).then { examplePhrases }
        assert(homeViewModel.state.phrases.isEmpty())

        homeViewModel.onStart()
        advanceUntilIdle()

        verify(storage, times(1)).retrieveBIP39Phrases()
        assert(homeViewModel.state.phrases == examplePhrases)

        whenever(storage.retrieveBIP39Phrases()).then { emptyPhrases }

        homeViewModel.onStart()
        advanceUntilIdle()

        verify(storage, times(2)).retrieveBIP39Phrases()

        assert(homeViewModel.state.phrases == emptyPhrases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}