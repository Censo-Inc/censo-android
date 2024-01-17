package co.censo.censo

import co.censo.censo.presentation.lock_screen.LockScreenState
import co.censo.censo.presentation.lock_screen.LockScreenViewModel
import co.censo.censo.test_helper.mockReadyOwnerStateWithNullPolicySetup
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.CompleteOwnerApprovershipApiRequest
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.ConfirmApprovershipApiResponse
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.DeletePolicySetupApiResponse
import co.censo.shared.data.model.DeleteSeedPhraseApiResponse
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetImportEncryptedDataApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerProof
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RejectApproverVerificationApiResponse
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.ReplacePolicyShardsApiResponse
import co.censo.shared.data.model.ResetLoginIdApiResponse
import co.censo.shared.data.model.ResetToken
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.model.StoreSeedPhraseApiResponse
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiResponse
import co.censo.shared.data.model.SubmitPurchaseApiResponse
import co.censo.shared.data.model.TimelockApiResponse
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.repository.AuthState
import co.censo.shared.data.repository.CreatePolicyParams
import co.censo.shared.data.repository.EncryptedSeedPhrase
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.BIP39
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
class LockScreenViewModelTest : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var timer: VaultCountDownTimer

    @Mock
    lateinit var ownerRepository: OwnerRepository

    private lateinit var lockScreenViewModel: LockScreenViewModel

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

        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            val collector: FlowCollector<Resource<OwnerState>> = invocation.getArgument(0)
            launch {
                collector.emit(Resource.Error())
            }
            return@thenAnswer null
        }

        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus == LockScreenState.LockStatus.None)
    }

    @Test
    fun `call onCreate with unlocked access global owner state then VM should set lock status to unlocked`() = runTest {
        assertDefaultVMState()

        //Setup collector to be able to emit updates
        lateinit var collector: FlowCollector<Resource<OwnerState>>

        //Mock the collectOwnerState method and assign the FlowCollector to the local reference
        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            collector = invocation.getArgument(0)
            launch {
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup))
            }
            return@thenAnswer null
        }

        //Trigger onCreate
        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Unlocked)

        //Use the reference to collector to emit a new value
        collector.emit(Resource.Error())

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.None)
    }

    @Test
    fun `call onCreate with locked access global owner state then VM should set lock status to locked`() = runTest {
        assertDefaultVMState()

        //Setup collector to be able to emit updates
        lateinit var collector: FlowCollector<Resource<OwnerState>>

        //Mock the collectOwnerState method and assign the FlowCollector to the local reference
        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            collector = invocation.getArgument(0)
            launch {
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup.copy(unlockedForSeconds = null)))
            }
            return@thenAnswer null
        }

        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        assertTrue(lockScreenViewModel.state.lockStatus is LockScreenState.LockStatus.Locked)

        //Emit error and assert lock status is none
        collector.emit(Resource.Error())

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

        //Setup collector to be able to emit updates
        lateinit var collector: FlowCollector<Resource<OwnerState>>

        //Mock the collectOwnerState method and assign the FlowCollector to the local reference
        whenever(ownerRepository.collectOwnerState(any())).thenAnswer { invocation ->
            collector = invocation.getArgument(0)
            launch {
                //Initially we want locked state
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup.copy(unlockedForSeconds = null)))
            }
            return@thenAnswer null
        }

        whenever(ownerRepository.unlock(
            biometryVerificationId = mockVerificationId, biometryData = mockFacetecBiometry
        )).thenAnswer {

            //When unlock is called, we want to emit the updated state as well return the UnlockApiResponse data
            launch {
                collector.emit(Resource.Success(mockReadyOwnerStateWithPolicySetup))
            }

            Resource.Success(
                UnlockApiResponse(
                    ownerState = mockReadyOwnerStateWithPolicySetup,
                    scanResultBlob = BiometryScanResultBlob(value = "BB")
                )
            )
        }
        //endregion

        //Trigger onCreate
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
}