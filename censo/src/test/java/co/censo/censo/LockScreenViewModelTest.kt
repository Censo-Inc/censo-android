package co.censo.censo

import co.censo.censo.presentation.lock_screen.LockScreenState
import co.censo.censo.presentation.lock_screen.LockScreenViewModel
import co.censo.censo.test_helper.mockReadyOwnerStateWithNullPolicySetup
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LockScreenViewModelTest : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var timer: VaultCountDownTimer

    @Mock
    lateinit var ownerRepository: OwnerRepository

    private lateinit var lockScreenViewModel: LockScreenViewModel
//
//    private var ownerStateFlow =  MutableStateFlow<Resource<OwnerState>>(
//        Resource.Success(mockReadyOwnerStateWithPolicySetup)
//    )

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data
    //endregion

    //region Setup/Teardown
    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        lockScreenViewModel = LockScreenViewModel(
            timer = timer, ownerRepository = ownerRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused Tests
    @Test
    fun `call onCreate with null data in global owner state then VM should set lock status to none`() = runTest {
        assertDefaultVMState()

        //TODO: Update tests to use mock responses from ownerRepository
        whenever(ownerRepository.collectOwnerState(collector = {})).thenAnswer {
            Resource.Success(null)
        }


//        ownerStateFlow.tryEmit(Resource.Success(null))



        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus == LockScreenState.LockStatus.None)
    }

    @Test
    fun `call onCreate with unlocked access global owner state then VM should set lock status to unlocked`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.collectOwnerState(collector = {resource: Resource<OwnerState> -> })).thenAnswer {
            Resource.Success(mockReadyOwnerStateWithPolicySetup as OwnerState)
        }

//        ownerStateFlow.tryEmit(Resource.Success(mockReadyOwnerStateWithPolicySetup))

        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Unlocked)

        //Emit null owner state data and assert lock status is none
//        ownerStateFlow.tryEmit(Resource.Success(null))
        whenever(ownerRepository.collectOwnerState(collector = {})).thenAnswer {
            Resource.Error<OwnerState>()
        }

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.None)
    }

    @Test
    fun `call onCreate with locked access global owner state then VM should set lock status to unlocked`() = runTest {
        assertDefaultVMState()

//        ownerStateFlow.tryEmit(Resource.Success(mockReadyOwnerStateWithPolicySetup.copy(unlockedForSeconds = null)))

        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Locked)

        //Emit null owner state data and assert lock status is none
//        ownerStateFlow.tryEmit(Resource.Success(null))

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.None)
    }

    @Test
    fun `call onStop then VM should stop timer`() {
        assertDefaultVMState()

        lockScreenViewModel.onStop()

        Mockito.verify(timer, atLeastOnce()).stop()
    }

    @Test
    fun `call onUnlockExpired then VM should set locked state and retrieve owner state`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.retrieveUser()).thenAnswer {
            Resource.Success(
                GetOwnerUserApiResponse(
                    identityToken = IdentityToken("AA"),
                    ownerState = mockReadyOwnerStateWithPolicySetup
                )
            )
        }

        lockScreenViewModel.onUnlockExpired()

        testScheduler.advanceUntilIdle()

        Mockito.verify(ownerRepository, atLeastOnce()).retrieveUser()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Locked)
    }

    @Test
    fun `call initUnlock then VM should set unlock in progress to state`() {
        assertDefaultVMState()

        lockScreenViewModel.initUnlock()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.UnlockInProgress)
    }
    //endregion

    @Test
    fun `test unlock flow`() = runTest {
        assertDefaultVMState()

        //region Mock out facetec data and ownerRepository.unlock method
        val mockVerificationId = BiometryVerificationId(value = "AA")
        val mockFacetecBiometry = FacetecBiometry(
            faceScan = "AA",
            auditTrailImage = "AA",
            lowQualityAuditTrailImage = "AA",
            verificationId = mockVerificationId
        )

        whenever(ownerRepository.unlock(
            biometryVerificationId = mockVerificationId, biometryData = mockFacetecBiometry
        )).thenAnswer {
            Resource.Success(
                UnlockApiResponse(
                    ownerState = mockReadyOwnerStateWithPolicySetup,
                    scanResultBlob = BiometryScanResultBlob(value = "BB")
                )
            )
        }
        //endregion

        //Set locked state
//        ownerStateFlow.tryEmit(Resource.Success(mockReadyOwnerStateWithPolicySetup.copy(unlockedForSeconds = null)))

        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Locked)

        //Init unlock
        lockScreenViewModel.initUnlock()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.UnlockInProgress)

        //Call onFaceScanReady to mock user completing face scan
        lockScreenViewModel.onFaceScanReady(
            verificationId = mockVerificationId, facetecData = mockFacetecBiometry
        )

        //Assert that app is unlocked
        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Unlocked)
    }

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(LockScreenState(), lockScreenViewModel.state)
    }
    //endregion

    //region Helper methods
    //endregion
}