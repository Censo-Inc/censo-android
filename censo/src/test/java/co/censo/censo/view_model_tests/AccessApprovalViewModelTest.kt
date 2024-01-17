package co.censo.censo.view_model_tests

import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.access_approval.AccessApprovalState
import co.censo.censo.presentation.access_approval.AccessApprovalUIState
import co.censo.censo.presentation.access_approval.AccessApprovalViewModel
import co.censo.censo.presentation.access_approval.backupApprover
import co.censo.censo.presentation.access_approval.primaryApprover
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.censo.util.TestUtil
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiResponse
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
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
        System.setProperty(TestUtil.TEST_MODE, TestUtil.TEST_MODE_TRUE)
        Dispatchers.setMain(testDispatcher)

        accessApprovalViewModel = AccessApprovalViewModel(
            ownerRepository = ownerRepository, pollingVerificationTimer = pollingVerificationTimer
        )
    }

    @After
    fun tearDown() {
        System.clearProperty(TestUtil.TEST_MODE)
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
    fun `call onStop then VM should stop polling timer`() {
        assertDefaultVMState()

        accessApprovalViewModel.onStop()

        Mockito.verify(pollingVerificationTimer, atLeastOnce()).stopWithDelay(any())
    }

    @Test
    fun `call setNavigateBackToHome then VM should set state for navigating and clear timelock`() {
        assertDefaultVMState()

        accessApprovalViewModel.setNavigateBackToHome()

        assertFalse(accessApprovalViewModel.state.isTimelocked)
        assertTrue(accessApprovalViewModel.state.navigationResource is Resource.Success)

        //assert that nav destination is OwnerVaultScreen
        assertEquals(
            Screen.OwnerVaultScreen.route,
            accessApprovalViewModel.state.navigationResource.asSuccess().data.route
        )
    }

    @Test
    fun `call initiateAccess then VM should hit initiate access endpoint`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.initiateAccess(accessPhraseIntent)).thenAnswer {
            Resource.Success(InitiateAccessApiResponse(mockReadyOwnerStateWithPolicySetup))
        }

        //Trigger onStart to set access intent to state
        accessApprovalViewModel.onStart(accessPhraseIntent)

        testScheduler.advanceUntilIdle()

        //Trigger initiate access
        accessApprovalViewModel.initiateAccess()

        testScheduler.advanceUntilIdle()

        assertEquals(accessPhraseIntent, accessApprovalViewModel.state.accessIntent)
        assertTrue(accessApprovalViewModel.state.initiateAccessResource is Resource.Success)
        assertFalse(accessApprovalViewModel.state.initiateNewAccess)
    }

    @Test
    fun `call cancelAccess then VM should hit cancel access endpoint`() = runTest {
        assertDefaultVMState()

        whenever(ownerRepository.cancelAccess()).thenAnswer {
            Resource.Success(DeleteAccessApiResponse(mockReadyOwnerStateWithPolicySetup))
        }

        accessApprovalViewModel.cancelAccess()
        //Assert that the cancel confirmation dialog is no longer displayed
        assertFalse(accessApprovalViewModel.state.showCancelConfirmationDialog)

        testScheduler.advanceUntilIdle()

        assertNull(accessApprovalViewModel.state.access)
        assertTrue(accessApprovalViewModel.state.navigationResource is Resource.Success)
        assertTrue(accessApprovalViewModel.state.cancelAccessResource is Resource.Success)

        //assert that nav destination is OwnerVaultScreen
        assertEquals(
            Screen.OwnerVaultScreen.route,
            accessApprovalViewModel.state.navigationResource.asSuccess().data.route
        )

    }

    @Test
    fun `call updateVerificationCode with 6 digit code then VM should update the verification code in state and call submit code endpoint `() = runTest {
        assertDefaultVMState()

        val primaryApprover = mockReadyOwnerStateWithPolicySetup.policy.approvers.primaryApprover()!!

        whenever(ownerRepository.submitAccessTotpVerification(primaryApprover.participantId, "123456")).thenAnswer {
            Resource.Success(SubmitAccessTotpVerificationApiResponse(
                mockReadyOwnerStateWithPolicySetup))
        }

        //Select approver to get participantId in state
        accessApprovalViewModel.onApproverSelected(primaryApprover)

        //Update verification with 1 digit at a time until 6 digits is reached
        var mockVerificationCode = 1.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        mockVerificationCode = 12.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        mockVerificationCode = 123.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        mockVerificationCode = 1234.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        mockVerificationCode = 12345.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        mockVerificationCode = 123456.toString()

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertEquals(mockVerificationCode, accessApprovalViewModel.state.verificationCode)

        testScheduler.advanceUntilIdle()

        Mockito.verify(ownerRepository, atLeastOnce()).submitAccessTotpVerification(primaryApprover.participantId, "123456")
        assertTrue(accessApprovalViewModel.state.submitTotpVerificationResource is Resource.Success)
        assertTrue(accessApprovalViewModel.state.waitingForApproval)
    }

    @Test
    fun `call updateVerificationCode with non digit string then VM should not set state`() {
        assertDefaultVMState()

        var mockVerificationCode = "String"

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertTrue(accessApprovalViewModel.state.verificationCode.isEmpty())

        mockVerificationCode = "123Digi"

        accessApprovalViewModel.updateVerificationCode(mockVerificationCode)
        assertTrue(accessApprovalViewModel.state.verificationCode.isEmpty())
    }

    @Test
    fun `call onContinue then VM should set UI state to ApproveAccess`() {
        assertDefaultVMState()

        accessApprovalViewModel.onContinue()

        assertTrue(accessApprovalViewModel.state.accessApprovalUIState == AccessApprovalUIState.ApproveAccess)
    }

    @Test
    fun `call onApproverSelected then VM should set approver to state`() {
        assertDefaultVMState()

        val primaryApprover = mockReadyOwnerStateWithPolicySetup.policy.approvers.primaryApprover()!!
        val backupApprover = mockReadyOwnerStateWithPolicySetup.policy.approvers.backupApprover()!!

        //Select primary approver and assert
        accessApprovalViewModel.onApproverSelected(primaryApprover)
        assertEquals(primaryApprover, accessApprovalViewModel.state.selectedApprover)

        //Selecting the same approver as the one in state, should set the state property to null
        accessApprovalViewModel.onApproverSelected(primaryApprover)
        assertNull(accessApprovalViewModel.state.selectedApprover)

        //Select backup approver and assert
        accessApprovalViewModel.onApproverSelected(backupApprover)
        assertEquals(backupApprover, accessApprovalViewModel.state.selectedApprover)
    }

    @Test
    fun `call onBackClicked then VM should react accordingly based on UI state`() {
        assertDefaultVMState()

        //Set UI state to SelectApprover
        accessApprovalViewModel.setUIState(AccessApprovalUIState.SelectApprover)

        accessApprovalViewModel.onBackClicked()

        assertTrue(accessApprovalViewModel.state.showCancelConfirmationDialog)

        //Call hideCloseConfirmationDialog to clear state property
        accessApprovalViewModel.hideCloseConfirmationDialog()
        assertFalse(accessApprovalViewModel.state.showCancelConfirmationDialog)

        //Set UI state to ApproveAccess
        accessApprovalViewModel.setUIState(AccessApprovalUIState.ApproveAccess)

        accessApprovalViewModel.onBackClicked()

        assertTrue(accessApprovalViewModel.state.accessApprovalUIState == AccessApprovalUIState.SelectApprover)

        //Set UI state to Approved
        accessApprovalViewModel.setUIState(AccessApprovalUIState.Approved)

        accessApprovalViewModel.onBackClicked()

        assertTrue(accessApprovalViewModel.state.accessApprovalUIState == AccessApprovalUIState.ApproveAccess)

    }

    @Test
    fun `call navigateIntentAware then VM should set navigation state accordingly based on access intent`() {
        assertDefaultVMState()

        //Set acccess phrase intent
        accessApprovalViewModel.onStart(accessPhraseIntent)

        accessApprovalViewModel.navigateIntentAware()

        assertEquals(Screen.AccessSeedPhrases.route, accessApprovalViewModel.state.navigationResource.asSuccess().data.route)

        //Reset navigation resource
        accessApprovalViewModel.resetNavigationResource()

        //Set replace policy intent
        accessApprovalViewModel.onStart(replacePolicyIntent)

        accessApprovalViewModel.navigateIntentAware()

        assertEquals(Screen.PolicySetupRoute.removeApproversRoute(), accessApprovalViewModel.state.navigationResource.asSuccess().data.route)

        //Reset navigation resource
        accessApprovalViewModel.resetNavigationResource()

        //Set recover owner key intent
        accessApprovalViewModel.onStart(recoverOwnerKeyIntent)

        accessApprovalViewModel.navigateIntentAware()

        assertEquals(Screen.OwnerKeyRecoveryRoute.route, accessApprovalViewModel.state.navigationResource.asSuccess().data.route)
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