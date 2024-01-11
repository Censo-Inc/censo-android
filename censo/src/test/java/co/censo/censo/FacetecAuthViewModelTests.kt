package co.censo.censo

import InitBiometryVerificationApiResponse
import co.censo.censo.data.repository.FacetecRepository
import co.censo.censo.presentation.facetec_auth.FacetecAuthState
import co.censo.censo.presentation.facetec_auth.FacetecAuthViewModel
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

@OptIn(ExperimentalCoroutinesApi::class)
class FacetecAuthViewModelTests : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var facetecRepository: FacetecRepository

    private lateinit var facetecAuthViewModel: FacetecAuthViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data
    private val mockOnFaceScanReady: suspend (
        biometryVerificationId: BiometryVerificationId,
        facetecBiometry: FacetecBiometry
    ) -> Resource<BiometryScanResultBlob> = { _, _ ->
        Resource.Success(BiometryScanResultBlob("mockScanResultBlob"))
    }

    private val mockInitBiometryVerificationApiResponse = InitBiometryVerificationApiResponse(
        id = BiometryVerificationId(value = ""),
        sessionToken = "",
        productionKeyText = "",
        deviceKeyId = "",
        biometryEncryptionPublicKey = ""
    )
    //endregion

    //region Setup/Teardown
    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        facetecAuthViewModel = FacetecAuthViewModel(
            facetecRepository = facetecRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused Tests
    @Test
    fun `call onStart with callback then VM should initiate a facetec session`() = runTest {
        assertDefaultVMState()

        callOnStartAndAssertThatFacetecSessionWasInitialized()
    }

    @Test
    fun `call facetecSDKInitialized then VM should set state for starting facetec authorization`() {
        assertDefaultVMState()

        facetecAuthViewModel.facetecSDKInitialized()

        //Assert that state was set for starting facetec auth
        assertTrue(facetecAuthViewModel.state.startAuth is Resource.Success)
        assertTrue(facetecAuthViewModel.state.submitResultResponse is Resource.Loading)
    }

    @Test
    fun `call simulateFacetecScanSuccess then VM should skip facetec auth`() = runTest {
        assertDefaultVMState()

        //Mock facetecRepository.startFacetecBiometry
        whenever(facetecRepository.startFacetecBiometry()).thenAnswer {
            Resource.Success(mockInitBiometryVerificationApiResponse)
        }

        //Pass in mockOnFaceScanReady to VM and trigger simulateFacetecScanSuccess
        facetecAuthViewModel.onStart(mockOnFaceScanReady)

        facetecAuthViewModel.simulateFacetecScanSuccess()

        testScheduler.advanceUntilIdle()

        assertTrue(facetecAuthViewModel.state.submitResultResponse is Resource.Uninitialized)
    }
    //endregion

    //region Flow Tests
    @Test
    fun `set error for initializing facetecSDK and attempt retry then VM should re-init facetec data`() = runTest {
        assertDefaultVMState()

        //Mock facetecRepository.startFacetecBiometry
        whenever(facetecRepository.startFacetecBiometry()).thenAnswer {
            Resource.Success(mockInitBiometryVerificationApiResponse)
        }

        facetecAuthViewModel.failedToInitializeSDK()

        assertTrue(facetecAuthViewModel.state.initFacetecData is Resource.Error)

        facetecAuthViewModel.retry()

        testScheduler.advanceUntilIdle()

        //Assert data was set to state successfully
        assertTrue(facetecAuthViewModel.state.initFacetecData is Resource.Success)
        assertEquals(mockInitBiometryVerificationApiResponse, facetecAuthViewModel.state.facetecData)
    }

    @Test
    fun `test full facetec auth success flow`() = runTest {
        assertDefaultVMState()

        callOnStartAndAssertThatFacetecSessionWasInitialized()

        //Call facetecSDKInitialized to simulate SDK being initialized
        facetecAuthViewModel.facetecSDKInitialized()

        //Assert that state was set for starting facetec auth
        assertTrue(facetecAuthViewModel.state.startAuth is Resource.Success)
        assertTrue(facetecAuthViewModel.state.submitResultResponse is Resource.Loading)

        facetecAuthViewModel.simulateFacetecScanSuccess()

        testScheduler.advanceUntilIdle()

        //Assert that submitResultResponse is Uninitialized (aka onFaceScanReady callback was triggered and flow is done)
        assertTrue(facetecAuthViewModel.state.submitResultResponse is Resource.Uninitialized)
    }
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(FacetecAuthState(), facetecAuthViewModel.state)
    }
    //endregion

    //region Helper methods
    private suspend fun callOnStartAndAssertThatFacetecSessionWasInitialized() {
        assertDefaultVMState()

        //Mock facetecRepository.startFacetecBiometry
        whenever(facetecRepository.startFacetecBiometry()).thenAnswer {
            Resource.Success(mockInitBiometryVerificationApiResponse)
        }

        facetecAuthViewModel.onStart(mockOnFaceScanReady)

        testDispatcher.scheduler.advanceUntilIdle()

        //Assert data was set to state successfully
        assertTrue(facetecAuthViewModel.state.initFacetecData is Resource.Success)
        assertEquals(mockInitBiometryVerificationApiResponse, facetecAuthViewModel.state.facetecData)
    }
    //endregion


}