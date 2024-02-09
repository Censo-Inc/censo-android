package co.censo.censo.view_model_tests

import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudAccessEnforcerState
import co.censo.shared.presentation.cloud_storage.CloudAccessEnforcerViewModel
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
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
class CloudAccessEnforcerViewModelTest : BaseViewModelTest() {
    
    //region Mocks and test objects
    @Mock
    lateinit var keyRepository: KeyRepository

    private lateinit var cloudAccessEnforcerViewModel: CloudAccessEnforcerViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region setup tear down
    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        cloudAccessEnforcerViewModel = CloudAccessEnforcerViewModel(
            keyRepository = keyRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused tests
    @Test
    fun `call onStart then VM should start collecting cloud access state`() = runTest {
        assertDefaultVMState()

        whenever(keyRepository.collectCloudAccessState(any())).thenAnswer { invocation ->
            val collector: FlowCollector<CloudAccessState> = invocation.getArgument(0)
            launch {
                collector.emit(CloudAccessState.Uninitialized)
            }
            return@thenAnswer null
        }

        cloudAccessEnforcerViewModel.onStart()

        testScheduler.advanceUntilIdle()

        //Check that state has not changed, since we only collected CloudAccessState.Uninitialized
        assertDefaultVMState()
        Mockito.verify(keyRepository, atLeastOnce()).collectCloudAccessState(any())
    }

    @Test
    fun `call onStart with AccessRequired cloud access state then VM should enforce access`() = runTest {
        assertDefaultVMState()

        lateinit var collector: FlowCollector<CloudAccessState>

        whenever(keyRepository.collectCloudAccessState(any())).thenAnswer { invocation ->
            collector = invocation.getArgument(0)
            launch {
                collector.emit(CloudAccessState.AccessRequired)
            }
            return@thenAnswer null
        }

        whenever(keyRepository.updateCloudAccessState(CloudAccessState.AccessGranted)).thenAnswer {
            launch { collector.emit(CloudAccessState.AccessGranted) }
            return@thenAnswer null
        }

        cloudAccessEnforcerViewModel.onStart()

        testScheduler.advanceUntilIdle()

        assertTrue(cloudAccessEnforcerViewModel.state.enforceAccess)
        Mockito.verify(keyRepository, atLeastOnce()).collectCloudAccessState(any())

        //Call onAccessGranted and assert that enforceAccess is false
        cloudAccessEnforcerViewModel.onAccessGranted()

        testScheduler.advanceUntilIdle()

        assertFalse(cloudAccessEnforcerViewModel.state.enforceAccess)
        Mockito.verify(keyRepository, atLeastOnce()).updateCloudAccessState(CloudAccessState.AccessGranted)
    }
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(CloudAccessEnforcerState(), cloudAccessEnforcerViewModel.state)
    }
    //endregion
}