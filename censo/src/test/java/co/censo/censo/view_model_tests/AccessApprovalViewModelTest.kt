package co.censo.censo.view_model_tests

import co.censo.censo.presentation.access_approval.AccessApprovalState
import co.censo.censo.presentation.access_approval.AccessApprovalViewModel
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerViewModel
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AccessApprovalViewModelTest : BaseViewModelTest() {

    //region Mocks
    @Mock
    lateinit var ownerRepository: OwnerRepository

    @Mock
    lateinit var pollingVerificationTimer: VaultCountDownTimer

    private lateinit var accessApprovalViewModel: AccessApprovalViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data

    private val accessPhraseIntent = AccessIntent.AccessPhrases
    private val replacePolicyIntent = AccessIntent.ReplacePolicy
    private val recoverOwnerKeyIntent = AccessIntent.RecoverOwnerKey
    //endregion

    //region Setup/Teardown
    @Before
    override fun setUp() {
        super.setUp()
        //TODO: Add set test mode property if applicable
        //TODO: List out test cases and PROGRESS

        Dispatchers.setMain(testDispatcher)

        accessApprovalViewModel = AccessApprovalViewModel(
            ownerRepository = ownerRepository, pollingVerificationTimer = pollingVerificationTimer
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
    fun `call onStart with access intent then VM should set state, get owner state, and start timer`() = runTest {
        assertDefaultVMState()

        accessApprovalViewModel.onStart(accessPhraseIntent)

        testScheduler.advanceUntilIdle()

        //Assert accessIntent was set, timer was started, and owner state was retrieved
        assertEquals(accessPhraseIntent, accessApprovalViewModel.state.accessIntent)

        Mockito.verify(ownerRepository, atLeastOnce()).getOwnerStateValue()
        Mockito.verify(pollingVerificationTimer, atLeastOnce()).startWithDelay(
            initialDelay = any(), interval = any(), onTickCallback = any()
        )

        //Call on start with alternate AccessIntent params and assert
        accessApprovalViewModel.onStart(replacePolicyIntent)
        assertEquals(replacePolicyIntent, accessApprovalViewModel.state.accessIntent)

        //Call onStart with recover owner key intent
        accessApprovalViewModel.onStart(recoverOwnerKeyIntent)
        assertEquals(recoverOwnerKeyIntent, accessApprovalViewModel.state.accessIntent)
    }

    @Test
    fun `foo2`() {
        //TODO: onStop calls pollingTimer onStop
    }

    @Test
    fun `foo3`() {
        //TODO: set navigateback home sets state + resetNavigationResource
    }

    @Test
    fun `foo4`() {
        //TODO: initiate access
    }

    @Test
    fun `foo5`() {
        //TODO: cancel access
    }

    @Test
    fun `foo6`() {
        //TODO: update verification code
    }

    @Test
    fun `foo7`() {
        //TODO: onContinue
    }

    @Test
    fun `foo8`() {
        //TODO: onApproverSelected
    }

    @Test
    fun `foo9`() {
        //TODO: onBackClick
    }

    @Test
    fun `foo10`() {
        //TODO: navIntentAware
    }
    //endregion

    //region Flow Tests
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(AccessApprovalState(), accessApprovalViewModel.state)
    }
    //endregion

    //region Helper methods
    //endregion
}