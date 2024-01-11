package co.censo.censo

import co.censo.censo.presentation.lock_screen.LockScreenState
import co.censo.censo.presentation.lock_screen.LockScreenViewModel
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
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

@OptIn(ExperimentalCoroutinesApi::class)
class LockScreenViewModelTest : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var timer: VaultCountDownTimer

    @Mock
    lateinit var ownerRepository: OwnerRepository

    @Mock
    lateinit var ownerStateFlow: MutableStateFlow<Resource<OwnerState>>

    private lateinit var lockScreenViewModel: LockScreenViewModel

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

        lockScreenViewModel = LockScreenViewModel(
            timer = timer, ownerRepository = ownerRepository, ownerStateFlow = ownerStateFlow
        )
    }

    @After
    fun tearDown() {
        //TODO: Add clear test mode property if applicable
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused Tests
    @Test
    fun `call onCreate then VM should start collection from global owner state flow`() = runTest {
        assertDefaultVMState()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

        }
        //TODO: How do we mock out or test mutableStateFlow here?
        // Maybe we want to use an actual implementation and not mock it out?
        lockScreenViewModel.onCreate()

        testScheduler.advanceUntilIdle()

        Mockito.verify(ownerStateFlow, atLeastOnce()).collect {}
    }
    //endregion

    //region Flow Tests
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(LockScreenState(), lockScreenViewModel.state)
    }
    //endregion

    //region Helper methods
    //endregion
}