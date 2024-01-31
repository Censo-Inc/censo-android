package co.censo.censo.view_model_tests

import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.presentation.plan_setup.PolicySetupScreenAction
import co.censo.censo.presentation.plan_setup.PolicySetupState
import co.censo.censo.presentation.plan_setup.PolicySetupUIState
import co.censo.censo.presentation.plan_setup.PolicySetupViewModel
import co.censo.censo.test_helper.initialProspectPrimaryApprover
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.censo.test_helper.prospectOwnerApprover
import co.censo.censo.util.TestUtil.TEST_MODE
import co.censo.censo.util.TestUtil.TEST_MODE_TRUE
import co.censo.censo.util.ownerApprover
import co.censo.censo.util.primaryApprover
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.PolicySetup
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.whenever
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
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class PolicySetupViewModelTest : BaseViewModelTest() {

    //region Mocks and test objects
    @Mock
    lateinit var ownerRepository: OwnerRepository

    @Mock
    lateinit var keyRepository: KeyRepository

    @Mock
    lateinit var verificationCodeTimer: VaultCountDownTimer

    @Mock
    lateinit var pollingVerificationTimer: VaultCountDownTimer

    @Mock
    lateinit var totpGenerator: TotpGenerator

    private lateinit var policySetupViewModel: PolicySetupViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data
    private val addApproversPolicySetupAction = PolicySetupAction.AddApprovers
    private val removeApproversPolicySetupAction = PolicySetupAction.RemoveApprovers

    private val addApproversThreshold = 2U

    private val readyOwnerStateDataWithOwnerAndInitialPrimaryApprover = mockReadyOwnerStateWithPolicySetup.copy(
        policySetup = PolicySetup(approvers = listOf(prospectOwnerApprover, initialProspectPrimaryApprover), threshold = addApproversThreshold)
    )

    private val readyOwnerStateDataWithAllApproversConfirmed = mockReadyOwnerStateWithPolicySetup

    private val readyStateOwnerUserWithInitialApproverMockResponse = GetOwnerUserApiResponse(
        identityToken = IdentityToken(value = "identityToken"), ownerState = readyOwnerStateDataWithOwnerAndInitialPrimaryApprover
    )

    private val genericTotpCode = "101010"
    //endregion

    //region Setup Teardown
    @Before
    override fun setUp() {
        super.setUp()
        System.setProperty(TEST_MODE, TEST_MODE_TRUE)

        Dispatchers.setMain(testDispatcher)

        policySetupViewModel = PolicySetupViewModel(
            ownerRepository = ownerRepository,
            keyRepository = keyRepository,
            verificationCodeTimer = verificationCodeTimer,
            pollingVerificationTimer = pollingVerificationTimer,
            totpGenerator = totpGenerator,
        )
    }

    @After
    fun tearDown() {
        System.clearProperty(TEST_MODE)
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused tests
    @Test
    fun `call onCreate with supplied parameter then VM should set policySetupAction to state`() {
        assertDefaultVMState()

        //Set and assert RemoveApprovers to state value
        policySetupViewModel.onCreate(removeApproversPolicySetupAction)

        assertEquals(
            removeApproversPolicySetupAction,
            policySetupViewModel.state.policySetupAction
        )

        //Set and assert AddApprovers to state value
        policySetupViewModel.onCreate(addApproversPolicySetupAction)

        assertEquals(
            addApproversPolicySetupAction,
            policySetupViewModel.state.policySetupAction
        )
    }

    @Test
    fun `call onStop then VM should call polling timer stop methods`() {
        assertDefaultVMState()

        policySetupViewModel.onStop()

        Mockito.verify(verificationCodeTimer, atLeastOnce()).stop()
        Mockito.verify(pollingVerificationTimer, atLeastOnce()).stopWithDelay(any())
    }

    @Test
    fun `receive updated name value then VM should set new value to state`() {
        assertDefaultVMState()

        val testName1 = "John"
        val testName2 = "Jane"

        //Set name 1 and assert
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.ApproverNicknameChanged(testName1))
        assertEquals(testName1, policySetupViewModel.state.editedNickname)

        //Clear name entry and assert
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.ApproverNicknameChanged(""))
        assertTrue(policySetupViewModel.state.editedNickname.isEmpty())

        //Set name 2 and assert
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.ApproverNicknameChanged(testName2))
        assertEquals(testName2, policySetupViewModel.state.editedNickname)
    }

    @Test
    fun `receive edit name action then VM should set editing state`() = runTest {
        assertDefaultVMState()

        setMockApproverDataToViewModelState()

        testScheduler.advanceUntilIdle()

        //Trigger edit action
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.EditApproverNickname)

        //Assert that the approver name for editing was set to state
        val expectedNameForEditing = readyOwnerStateDataWithOwnerAndInitialPrimaryApprover.policySetup?.approvers?.primaryApprover()?.label ?: ""
        assertEquals(expectedNameForEditing, policySetupViewModel.state.editedNickname)

        //Simulate user deleting last character in name and assert that the new value is set to state
        val editedNameMinusLastCharacter = expectedNameForEditing.dropLast(1)
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.ApproverNicknameChanged(editedNameMinusLastCharacter))
        assertEquals(editedNameMinusLastCharacter, policySetupViewModel.state.editedNickname)
    }

    @Test
    fun `receive go live with approver action then VM should set UI state for approver activation`() {
        assertDefaultVMState()

        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.GoLiveWithApprover)

        assertEquals(PolicySetupUIState.ApproverActivation_5, policySetupViewModel.state.policySetupUIState)
    }

    @Test
    fun `receive back clicked action then VM should react based on UI state`() {
        assertDefaultVMState()

        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)

        //Assert for no change in state since UI state should be set to Initial_1
        assertDefaultVMState()

        //Set back arrow icon to state via EditApproverNickname_3
        policySetupViewModel.setUIState(PolicySetupUIState.EditApproverNickname_3)
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)

        //Assert that UIState is ApproverActivation_5
        assertEquals(PolicySetupUIState.ApproverActivation_5, policySetupViewModel.state.policySetupUIState)

        //Set back arrow icon to state via ApproverActivation_5
        policySetupViewModel.setUIState(PolicySetupUIState.ApproverActivation_5)
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)

        //Assert that UIState is ApproverGettingLive_4
        assertEquals(PolicySetupUIState.ApproverGettingLive_4, policySetupViewModel.state.policySetupUIState)

        //Set exit icon to state via ApproverGettingLive_4
        policySetupViewModel.setUIState(PolicySetupUIState.ApproverGettingLive_4)
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)

        //Assert that navigationResource was set to state and route is OwnerVaultScreen
        assertTrue(policySetupViewModel.state.navigationResource is Resource.Success)
        assertEquals(Screen.OwnerVaultScreen.navToAndPopCurrentDestination().route, policySetupViewModel.state.navigationResource.success()?.data?.route)
    }

    @Test
    fun `receive retry action then VM should retrieve remote owner state and update local owner state`() = runTest {
        assertDefaultVMState()

        //region Mocks
        whenever(ownerRepository.retrieveUser()).thenAnswer {
            Resource.Success(readyStateOwnerUserWithInitialApproverMockResponse)
        }

        whenever(keyRepository.decryptWithDeviceKey(any())).thenAnswer {
            byteArrayOf(Byte.MAX_VALUE)
        }

        whenever(totpGenerator.generateCode(any(), any())).thenAnswer {
            genericTotpCode
        }
        //endregion

        //Trigger retry
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.Retry)

        testScheduler.advanceUntilIdle()

        //Verify that retrieveUser was called
        Mockito.verify(ownerRepository, atLeastOnce()).retrieveUser()

        //Assert approver data was set to state
        assertEquals(readyOwnerStateDataWithOwnerAndInitialPrimaryApprover.policySetup?.approvers?.ownerApprover(), policySetupViewModel.state.ownerApprover)
        assertEquals(readyOwnerStateDataWithOwnerAndInitialPrimaryApprover.policySetup?.approvers?.primaryApprover(), policySetupViewModel.state.primaryApprover)
    }

    @Test
    fun `receive approver confirmed action with all approvers confirmed then VM should trigger replace policy`() = runTest {
        assertDefaultVMState()

        //region Mocks
        whenever(ownerRepository.getOwnerStateValue()).thenAnswer {
            readyOwnerStateDataWithAllApproversConfirmed
        }

        whenever(keyRepository.decryptWithDeviceKey(any())).thenAnswer {
            byteArrayOf(Byte.MAX_VALUE)
        }

        whenever(totpGenerator.generateCode(any(), any())).thenAnswer {
            genericTotpCode
        }
        //endregion

        policySetupViewModel.onCreate(PolicySetupAction.AddApprovers)
        policySetupViewModel.onResume()

        testScheduler.advanceUntilIdle()

        //Trigger action
        policySetupViewModel.receivePlanAction(PolicySetupScreenAction.ApproverConfirmed)


        //Assert that state was set
        //Verify that pollingVerificationTimer.stop is called
        assertTrue(policySetupViewModel.state.replacePolicy is Resource.Success)
        assertEquals(PolicySetupUIState.Uninitialized_0, policySetupViewModel.state.policySetupUIState)
        Mockito.verify(pollingVerificationTimer, atLeastOnce()).stop()
    }
    //endregion

    //region Flow tests
    @Test
    fun `start VM with AddApprover action then VM should set state and start polling timers`() = runTest {
        assertDefaultVMState()

        setMockApproverDataToViewModelState()

        testScheduler.advanceUntilIdle()

        //Verify both polling timers were started
        Mockito.verify(pollingVerificationTimer, atLeastOnce()).start(
            interval = any(), skipFirstTick = eq(true), onTickCallback = any()
        )
        Mockito.verify(verificationCodeTimer, atLeastOnce()).start(any(), eq(false), any())
    }

    @Test
    fun `start VM with RemoveApprover action then VM should submit policy setup and trigger replace policy navigation`() = runTest {
        assertDefaultVMState()

        //region Mock ownerRepository.createPolicySetup, keyRepository.decryptWithDeviceKey, and totpGenerator.generateCode
        whenever(
            ownerRepository.createPolicySetup(
                threshold = ArgumentMatchers.anyInt().toUInt(),
                approvers = ArgumentMatchers.anyList()
            )
        ).thenAnswer {
            Resource.Success(
                CreatePolicySetupApiResponse(
                    ownerState = readyOwnerStateDataWithOwnerAndInitialPrimaryApprover
                )
            )
        }

        whenever(keyRepository.decryptWithDeviceKey(any())).thenAnswer {
            byteArrayOf(Byte.MAX_VALUE)
        }

        whenever(totpGenerator.generateCode(any(), any())).thenAnswer {
            genericTotpCode
        }
        //endregion

        //Trigger VM
        policySetupViewModel.onCreate(removeApproversPolicySetupAction)

        testScheduler.advanceUntilIdle()

        //Verify that ownerStateFlow.tryEmit was called
        Mockito.verify(ownerRepository, atLeastOnce()).updateOwnerState(any())

        //Assert createPolicySetupResponse is success and has data
        assertTrue(policySetupViewModel.state.createPolicySetupResponse is Resource.Success)
        assertEquals(readyOwnerStateDataWithOwnerAndInitialPrimaryApprover, policySetupViewModel.state.createPolicySetupResponse.success()?.data?.ownerState)

        //Assert replacePolicy is success and UIState is set to uninitialized
        assertTrue(policySetupViewModel.state.replacePolicy is Resource.Success)
        assertEquals(PolicySetupUIState.Uninitialized_0, policySetupViewModel.state.policySetupUIState)

        //Reset replacePolicy to mimic the LaunchedEffect of the screen side
        policySetupViewModel.resetReplacePolicy()

        //Assert replacePolicy was reset
        assertTrue(policySetupViewModel.state.replacePolicy is Resource.Uninitialized)

        //Verify that the polling timers did not start since this is the RemoveApprovers flow
        Mockito.verify(pollingVerificationTimer, never()).start(
            interval = any(), skipFirstTick = any(), onTickCallback = any()
        )
        Mockito.verify(verificationCodeTimer, never()).start(any(), skipFirstTick = eq(true), any())
    }
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(PolicySetupState(), policySetupViewModel.state)
    }
    //endregion

    //region Helper methods
    private fun setMockApproverDataToViewModelState() {
        //region Mock ownerStateFlow.value, keyRepository.decryptWithDeviceKey, and totpGenerator.generateCode
        whenever(ownerRepository.getOwnerStateValue()).thenAnswer {
            readyOwnerStateDataWithOwnerAndInitialPrimaryApprover
        }

        whenever(keyRepository.decryptWithDeviceKey(any())).thenAnswer {
            byteArrayOf(Byte.MAX_VALUE)
        }

        whenever(totpGenerator.generateCode(any(), any())).thenAnswer {
            genericTotpCode
        }
        //endregion

        //Call onCreate then onResume to simulate flow
        policySetupViewModel.onCreate(addApproversPolicySetupAction)
        policySetupViewModel.onResume()

        //Assert for policySetupAction being set to state
        //Assert that policySetupUIState is set to ApproverNickname_2
        assertEquals(
            addApproversPolicySetupAction,
            policySetupViewModel.state.policySetupAction
        )

        assertEquals(
            PolicySetupUIState.ApproverNickname_2,
            policySetupViewModel.state.policySetupUIState
        )
    }
    //endregion
}