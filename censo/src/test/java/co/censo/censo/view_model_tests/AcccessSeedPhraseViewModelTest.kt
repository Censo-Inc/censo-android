package co.censo.censo.view_model_tests

import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.access_seed_phrases.AccessPhrasesUIState
import co.censo.censo.presentation.access_seed_phrases.AccessSeedPhrasesState
import co.censo.censo.presentation.access_seed_phrases.AccessSeedPhrasesViewModel
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.censo.test_helper.mockSeedPhrase
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.BIP39
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class AcccessSeedPhraseViewModelTest : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var ownerRepository: OwnerRepository

    @Mock
    lateinit var timer: VaultCountDownTimer

    private lateinit var accessSeedPhrasesViewModel: AccessSeedPhrasesViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data
    //endregion

    //region Setup/Teardown
    @Before
    override fun setUp() {
        super.setUp()
        //TODO: Add set test mode property if applicable

        Dispatchers.setMain(testDispatcher)

        accessSeedPhrasesViewModel = AccessSeedPhrasesViewModel(
            ownerRepository = ownerRepository, timer = timer
        )
    }

    @After
    fun tearDown() {
        //TODO: Add clear test mode property if applicable
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused Tests

    //TODO: List out test cases
    // onStart --------
    // onStop -------
    // reset --------
    // retrieveOwnerState? (maybe as a flow test)
    // cancelAccess -----
    // onBackClicked (will need to set UI state for these)
    // startFacetec ------
    // onPhraseSelected ------
    // onFaceScanReady

    //Flow test for
    // recoverSeedPhrases
    // cancelAccess
    // resetNavResource


    @Test
    fun `call onStart then VM should collect owner state and start timer`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            val collector: FlowCollector<Resource<OwnerState>> = invocation.getArgument(0)
            launch {
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup))
            }
            return@thenAnswer null
        }

        accessSeedPhrasesViewModel.onStart()

        testScheduler.advanceUntilIdle()

        assertTrue(accessSeedPhrasesViewModel.state.ownerState is Resource.Success)
        Mockito.verify(ownerRepository, atLeastOnce()).collectOwnerState(any())
        Mockito.verify(timer, atLeastOnce()).start(any(), any())
    }

    @Test
    fun `call onStop then VM should stop timer`() {
        assertDefaultVMState()

        accessSeedPhrasesViewModel.onStop()

        Mockito.verify(timer, atLeastOnce()).stop()
    }

    @Test
    fun `call reset then VM reset all state properties except ownerState`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            val collector: FlowCollector<Resource<OwnerState>> = invocation.getArgument(0)
            launch {
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup))
            }
            return@thenAnswer null
        }

        accessSeedPhrasesViewModel.onStart()

        testScheduler.advanceUntilIdle()

        assertTrue(accessSeedPhrasesViewModel.state.ownerState is Resource.Success)

        accessSeedPhrasesViewModel.reset()

        assertTrue(accessSeedPhrasesViewModel.state.ownerState is Resource.Success)
    }

    @Test
    fun `call onPhraseSelected then VM should set selected phrase and UI state`() {
        assertDefaultVMState()

        accessSeedPhrasesViewModel.onPhraseSelected(mockSeedPhrase)

        assertEquals(mockSeedPhrase, accessSeedPhrasesViewModel.state.selectedPhrase)
        assertTrue(accessSeedPhrasesViewModel.state.accessPhrasesUIState == AccessPhrasesUIState.ReadyToStart)
    }

    @Test
    fun `call startFacetec then VM should set selected language and UI state`() {
        assertDefaultVMState()

        val englishLanguage = BIP39.WordListLanguage.English

        accessSeedPhrasesViewModel.startFacetec(englishLanguage)

        assertEquals(englishLanguage, accessSeedPhrasesViewModel.state.currentLanguage)
        assertTrue(accessSeedPhrasesViewModel.state.accessPhrasesUIState == AccessPhrasesUIState.Facetec)

        //Set Czech then state should not be the same as previously selected langauge
        accessSeedPhrasesViewModel.startFacetec(BIP39.WordListLanguage.Czech)

        assertNotSame(englishLanguage, accessSeedPhrasesViewModel.state.currentLanguage)
    }

    @Test
    fun `call cancelAccess then VM should cancel access and set navigation state`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.cancelAccess()).thenAnswer {
            Resource.Success(DeleteAccessApiResponse(mockReadyOwnerStateWithPolicySetup))
        }

        accessSeedPhrasesViewModel.cancelAccess()

        testScheduler.advanceUntilIdle()

        //Assert nav resource and verify mocks were called
        assertEquals(Screen.OwnerVaultScreen.route, accessSeedPhrasesViewModel.state.navigationResource.asSuccess().data.route)
        assertTrue(accessSeedPhrasesViewModel.state.cancelAccessResource is Resource.Success)
        Mockito.verify(ownerRepository, atLeastOnce()).cancelAccess()
        Mockito.verify(ownerRepository, atLeastOnce()).updateOwnerState(any())
    }



    //endregion

    //region Flow Tests
    //endregion

    //region Custom asserts
    //endregion

    //region Helper methods
    private fun assertDefaultVMState() {
        assertEquals(AccessSeedPhrasesState(), accessSeedPhrasesViewModel.state)
    }
    //endregion
}