package co.censo.censo.view_model_tests

import Base64EncodedData
import ParticipantId
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.plan_finalization.ReplacePolicyAction
import co.censo.censo.presentation.plan_finalization.ReplacePolicyState
import co.censo.censo.presentation.plan_finalization.ReplacePolicyUIState
import co.censo.censo.presentation.plan_finalization.ReplacePolicyViewModel
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.test_helper.createApproverKey
import co.censo.censo.test_helper.generateEntropy
import co.censo.censo.test_helper.genericParticipantId
import co.censo.censo.test_helper.mockInitialOwnerStateData
import co.censo.censo.test_helper.mockReadyOwnerStateWithPolicySetup
import co.censo.censo.util.TestUtil.TEST_MODE
import co.censo.censo.util.TestUtil.TEST_MODE_TRUE
import co.censo.censo.util.alternateApprover
import co.censo.censo.util.getEntropyFromOwnerApprover
import co.censo.censo.util.ownerApprover
import co.censo.censo.util.primaryApprover
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.CompleteOwnerApprovershipApiRequest
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.IdentityToken
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

class ReplacePolicyViewModelTest : BaseViewModelTest() {

    //region Mocks and testing objects
    @Mock
    lateinit var ownerRepository: OwnerRepository

    @Mock
    lateinit var keyRepository: KeyRepository

    private lateinit var replacePolicyViewModel: ReplacePolicyViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region Testing data
    /**
     * Note, none of the test data below is used for encryption key work inside the ReplacePolicyVM.
     *
     * Outside of the encryption work we do when downloading a key,
     * (i.e. recreating the key pair from the loaded encrypted key),
     * all encryption work is behind the repository layer so we can mock out responses
     */

    private val addApproversPolicySetupAction = PolicySetupAction.AddApprovers
    private val removeApproversPolicySetupAction = PolicySetupAction.RemoveApprovers

    private val savedDeviceId = "123456789"

    private val readyOwnerStateData = mockReadyOwnerStateWithPolicySetup
    private val initialOwnerStateData = mockInitialOwnerStateData

    private val readyStateOwnerUserMockResponse = GetOwnerUserApiResponse(
        identityToken = IdentityToken(value = "identityToken"), ownerState = readyOwnerStateData
    )

    private val initialStateOwnerUserMockResponse = GetOwnerUserApiResponse(
        identityToken = IdentityToken(value = "identityToken"), ownerState = initialOwnerStateData
    )


    private val completeOwnerApprovershipMockResponse = CompleteOwnerApprovershipApiResponse(
        ownerState = readyOwnerStateData
    )

    private val initiateAccessMockResponse = InitiateAccessApiResponse(
        ownerState = readyOwnerStateData
    )

    private val retrieveShardsMockResponse = RetrieveAccessShardsApiResponse(
        ownerState = readyOwnerStateData,
        encryptedShards = listOf(EncryptedShard(
            participantId = genericParticipantId,
            encryptedShard = Base64EncodedData(base64Encoded = "CC"),
            isOwnerShard = true,
            ownerEntropy = generateEntropy(),
            approverPublicKey = null
        )),
        scanResultBlob = BiometryScanResultBlob(value = "Mock_ScanResultBlob")
    )

    //Mock Biometry data
    private val verificationId = BiometryVerificationId(value = "biometric_verification_id")
    private val mockBiometry = FacetecBiometry(
        faceScan = "Mock_FaceScan",
        auditTrailImage = "Mock_auditTrailImage",
        lowQualityAuditTrailImage = "Mock_lowQualityAuditTrailImage",
        verificationId = verificationId
    )
    //endregion

    //region Setup TearDown
    @Before
    override fun setUp() {
        super.setUp()
        System.setProperty(TEST_MODE, TEST_MODE_TRUE)

        Dispatchers.setMain(testDispatcher)

        replacePolicyViewModel = ReplacePolicyViewModel(
            ownerRepository = ownerRepository,
            keyRepository = keyRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        System.clearProperty(TEST_MODE)
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused Testing
    @Test
    fun `call onCreate with supplied parameter then VM should set policySetupAction to state`() {
        assertDefaultVMState()

        //Set and assert RemoveApprovers to state value
        replacePolicyViewModel.onCreate(removeApproversPolicySetupAction)

        assertEquals(replacePolicyViewModel.state.policySetupAction, removeApproversPolicySetupAction)

        //Set and assert AddApprovers to state value
        replacePolicyViewModel.onCreate(addApproversPolicySetupAction)

        assertEquals(replacePolicyViewModel.state.policySetupAction, addApproversPolicySetupAction)
    }

    @Test
    fun `call onCreate with OwnerState set to Initial then updateOwnerState should return early`() = runTest {
        assertDefaultVMState()

        //Set initial owner state user data for responses
        whenever(ownerRepository.getOwnerStateValue()).thenAnswer { initialOwnerStateData }
        whenever(ownerRepository.retrieveUser()).thenAnswer { Resource.Success(initialStateOwnerUserMockResponse) }

        replacePolicyViewModel.onCreate(addApproversPolicySetupAction)

        //Verify ownerStateFlow was not emitted to
        Mockito.verify(ownerRepository, times(0)).updateOwnerState(any())

        assertNull(replacePolicyViewModel.state.ownerApprover)
        assertNull(replacePolicyViewModel.state.primaryApprover)
        assertNull(replacePolicyViewModel.state.alternateApprover)
    }

    @Test
    fun `assert retry triggers owner state data refresh and checks user has saved key`() = runTest {
        assertDefaultVMState()

        setTrueResponseForKeyRepositoryMethodUserHasKeySavedInCloud()

        //Assert default state
        assertTrue(replacePolicyViewModel.state.userResponse is Resource.Uninitialized)

        //Set mock response for retrieve user call
        whenever(ownerRepository.retrieveUser()).thenAnswer { Resource.Success(readyStateOwnerUserMockResponse) }

        //Retry action received
        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.Retry)

        //advanceUntilIdle
        testScheduler.advanceUntilIdle()

        //Assert expected state
        assertTrue(replacePolicyViewModel.state.userResponse is Resource.Success)
        assertEquals(readyOwnerStateData, replacePolicyViewModel.state.userResponse.success()?.data)
    }

    @Test
    fun `assert back click when exit icon is displayed triggers navigation`() {
        assertDefaultVMState()

        //Set UI state that displays exit icon
        replacePolicyViewModel.setUIState(uiState = ReplacePolicyUIState.AccessInProgress_2)

        //Back click action received
        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.BackClicked)

        //Assert navigation is set to state
        assertTrue(replacePolicyViewModel.state.navigationResource is Resource.Success)
        assertEquals(Screen.OwnerVaultScreen.route, replacePolicyViewModel.state.navigationResource.success()?.data)

        //Reset navigation state and assert
        replacePolicyViewModel.resetNavigationResource()
        assertTrue(replacePolicyViewModel.state.navigationResource is Resource.Uninitialized)
    }

    @Test
    fun `assert back click when no icon is displayed does not trigger navigation`() {
        assertDefaultVMState()

        //Back click action received
        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.BackClicked)

        //Assert navigation is not set to state
        assertTrue(replacePolicyViewModel.state.navigationResource !is Resource.Success)
    }

    @Test
    fun `assert view model receives completed plan action and triggers navigation`() {
        assertDefaultVMState()

        //Complete action received
        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.Completed)

        //Assert navigation is set to state
        assertTrue(replacePolicyViewModel.state.navigationResource is Resource.Success)
        assertEquals(Screen.OwnerVaultScreen.route, replacePolicyViewModel.state.navigationResource.success()?.data)

        //Reset navigation state and assert
        replacePolicyViewModel.resetNavigationResource()
        assertTrue(replacePolicyViewModel.state.navigationResource is Resource.Uninitialized)
    }

    //TODO: Update this test in next PR to match new key usage pattern
//    @Test
//    fun `assert key is created and saved to state when no key data is in state`() = runTest {
//        assertDefaultVMState()
//
//        //Set necessary mocks
//        setFalseResponseForKeyRepositoryMethodUserHasKeySavedInCloud()
//
//        val approverKey = createApproverKey()
//        whenever(keyRepository.createApproverKey()).thenAnswer { approverKey }
//        whenever(keyRepository.retrieveSavedDeviceId()).thenAnswer { savedDeviceId }
//
//        //Set mocked global owner state data
//        whenever(ownerRepository.getOwnerStateValue()).thenAnswer { readyOwnerStateData }
//
//        //Trigger VM
//        replacePolicyViewModel.onCreate(addApproversPolicySetupAction)
//
//        testScheduler.advanceUntilIdle()
//
//        //Verify that global ownerStateFlow was emitted to
//        Mockito.verify(ownerRepository, atLeastOnce()).updateOwnerState(any())
//
//        //Assert that approver data was set to state
//        assertConfirmedProspectApproverDataSetToState()
//
//        //Verify that userHasKeySavedInCloud is called
//        Mockito.verify(keyRepository, times(1)).userHasKeySavedInCloud(genericParticipantId)
//
//        //Assert state is set
////        assertTrue(replacePolicyViewModel.state.cloudStorageAction.triggerAction)
////        assertEquals(CloudStorageActions.UPLOAD, replacePolicyViewModel.state.cloudStorageAction.action)
////        assertFalse(replacePolicyViewModel.state.saveKeyToCloud is Resource.Error)
//
//        assertNotNull(replacePolicyViewModel.state.keyData)
//    }
    //endregion

    //TODO: Update this test in next PR to match new key usage pattern
    /**
     * Test case: assert policy replacement logic flow results in success
     *
     * Goal of this test case is to mock out the repo responses and UI interactions during the full logic flow of replacing a policy.
     * Let this be a guideline to how the ReplacePolicyVM is expected to behave during a full success flow
     *
     * Prerequisites:
     *
     * - Set mock responses for the following repository calls:
     *      - KeyRepository.userHasKeySavedInCloud
     *      - KeyRepository.createApproverKey
     *      - KeyRepository.retrieveSavedDeviceId
     *      - OwnerRepository.completeApproverOwnership
     *      - OwnerRepository.initiateAccess
     *      - OwnerRepository.retrieveAccessShards
     *      - OwnerRepository.replacePolicy
     *      - OwnerRepository.verifyKeyConfirmationSignature
     *
     * Expected end outcome to assert for:
     *
     * - replacePolicyUIState should be Completed_3 once the policy has been replaced
     * - navigationResource is set to state after Completed PlanAction is received
     *
     */
//    @Test
//    fun `assert policy replacement logic flow results in success`() = runTest {
//        assertDefaultVMState()
//
//        //region Set initial mocks
//        setFalseResponseForKeyRepositoryMethodUserHasKeySavedInCloud()
//
//        val approverKey = createApproverKey()
//        whenever(keyRepository.createApproverKey()).thenAnswer { approverKey }
//        whenever(keyRepository.retrieveSavedDeviceId()).thenAnswer { savedDeviceId }
//
//        //Set mocked global owner state data
//        whenever(ownerRepository.getOwnerStateValue()).thenAnswer { readyOwnerStateData }
//        //endregion
//
//        //Trigger VM
//        replacePolicyViewModel.onCreate(addApproversPolicySetupAction)
//
//        testScheduler.advanceUntilIdle()
//
//        //Verify that global ownerStateFlow was emitted to
//        Mockito.verify(ownerRepository, atLeastOnce()).updateOwnerState(any())
//
//        //Assert that approver data was set to state
//        assertConfirmedProspectApproverDataSetToState()
//
//        //Verify that userHasKeySavedInCloud is called
//        Mockito.verify(keyRepository, times(1)).userHasKeySavedInCloud(genericParticipantId)
//
//        //Assert state is set
//        assertKeyUploadSetToState()
//        assertFalse(replacePolicyViewModel.state.saveKeyToCloud is Resource.Error)
//
//        assertNotNull(replacePolicyViewModel.state.keyData)
//
//        //region Set mocks for completeApprovership and initiateAccess repo methods
//        whenever(
//            ownerRepository.completeApproverOwnership(
//                participantId = genericParticipantId,
//                completeOwnerApprovershipApiRequest = CompleteOwnerApprovershipApiRequest(
//                    approverPublicKey = replacePolicyViewModel.state.keyData?.publicKey!!
//                )
//            )
//        ).thenAnswer {
//            Resource.Success(completeOwnerApprovershipMockResponse)
//        }
//
//        whenever(ownerRepository.initiateAccess(AccessIntent.ReplacePolicy)).thenAnswer {
//            Resource.Success(initiateAccessMockResponse)
//        }
//        //endregion
//
//        //Trigger VM key upload success
//        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.KeyUploadSuccess)
//
//        testScheduler.advanceUntilIdle()
//
//        //Assert for completeApprovershipResponse is success
//        //And initiateAccess state is set
//        assertTrue(replacePolicyViewModel.state.completeApprovershipResponse is Resource.Success)
//        //Assert data
//        assertEquals(completeOwnerApprovershipMockResponse, replacePolicyViewModel.state.completeApprovershipResponse.success()?.data)
//        assertTrue(replacePolicyViewModel.state.initiateAccessResponse is Resource.Success)
//        //assert data
//        assertEquals(initiateAccessMockResponse, replacePolicyViewModel.state.initiateAccessResponse.success()?.data)
//        assertTrue(replacePolicyViewModel.state.replacePolicyUIState == ReplacePolicyUIState.AccessInProgress_2)
//
//        //region Set mocks for retrieveAccessShards, replacePolicy, and verifyKeyConfirmationSignature repo methods
//        whenever(ownerRepository.retrieveAccessShards(verificationId, mockBiometry)).thenAnswer {
//            Resource.Success(retrieveShardsMockResponse)
//        }
//
//        whenever(
//            ownerRepository.replacePolicy(
//                encryptedIntermediatePrivateKeyShards = retrieveShardsMockResponse.encryptedShards,
//                encryptedMasterPrivateKey = readyOwnerStateData.policy.encryptedMasterKey,
//                threshold = replacePolicyViewModel.state.policySetupAction.threshold,
//                approvers = listOfNotNull(
//                    replacePolicyViewModel.state.ownerApprover,
//                    replacePolicyViewModel.state.primaryApprover,
//                    replacePolicyViewModel.state.alternateApprover
//                ),
//                ownerApproverEncryptedPrivateKey = replacePolicyViewModel.state.keyData!!.encryptedPrivateKey,
//                ownerApproverKey = replacePolicyViewModel.state.keyData!!.publicKey,
//                entropy = readyOwnerStateData.policySetup?.approvers!!.ownerApprover()!!.getEntropyFromOwnerApprover()!!,
//                deviceKeyId = savedDeviceId
//            )
//        ).thenAnswer {
//            Resource.Success(
//                ReplacePolicyApiResponse(
//                    ownerState = readyOwnerStateData
//                )
//            )
//        }
//
//        whenever(ownerRepository.verifyKeyConfirmationSignature(any())).thenAnswer { true }
//        //endregion
//
//        //Trigger next steps in logic with onFaceScanReady
//        replacePolicyViewModel.onFaceScanReady(verificationId, mockBiometry)
//
//        testScheduler.advanceUntilIdle()
//
//        Mockito.verify(ownerRepository, atLeastOnce()).cancelAccess()
//        assertTrue(replacePolicyViewModel.state.retrieveAccessShardsResponse is Resource.Success)
//        assertEquals(retrieveShardsMockResponse, replacePolicyViewModel.state.retrieveAccessShardsResponse.success()?.data)
//
//        //Assert for replace policy response being success
//        // Assert for replace policy ui state being Completed
//        assertTrue(replacePolicyViewModel.state.replacePolicyResponse is Resource.Success)
//        assertEquals(readyOwnerStateData, replacePolicyViewModel.state.replacePolicyResponse.success()?.data?.ownerState)
//
//        assertTrue(replacePolicyViewModel.state.replacePolicyUIState == ReplacePolicyUIState.Completed_3)
//
//
//        //Trigger completed plan action
//        replacePolicyViewModel.receivePlanAction(ReplacePolicyAction.Completed)
//
//        //Assert that navigation state is set
//        assertTrue(replacePolicyViewModel.state.navigationResource is Resource.Success)
//        assertEquals(Screen.OwnerVaultScreen.route, replacePolicyViewModel.state.navigationResource.success()?.data)
//    }


    //region Helper methods
    private suspend fun setTrueResponseForKeyRepositoryMethodUserHasKeySavedInCloud() {
        //Set this mocked response so that the VM hits the flow where we set state for triggering a key download
        whenever(
            keyRepository.userHasKeySavedInCloud(
                genericParticipantId.value
            )
        ).thenAnswer { true }
    }

    private suspend fun setFalseResponseForKeyRepositoryMethodUserHasKeySavedInCloud() {
        //Set this mocked response so that the VM hits the flow where we set state for triggering a key download
        whenever(
            keyRepository.userHasKeySavedInCloud(
                genericParticipantId.value
            )
        ).thenAnswer { false }
    }
    //endregion

    //region Custom Asserts
    private fun assertDefaultVMState() {
        assertEquals(ReplacePolicyState(), replacePolicyViewModel.state)
    }

    private fun assertConfirmedProspectApproverDataSetToState() {
        val approvers = readyOwnerStateData.policySetup?.approvers ?: emptyList()

        val ownerApprover = approvers.ownerApprover()
        val primaryApprover: Approver.ProspectApprover? = approvers.primaryApprover()
        val alternateApprover: Approver.ProspectApprover? = approvers.alternateApprover()

        assertEquals(ownerApprover, replacePolicyViewModel.state.ownerApprover)
        assertEquals(primaryApprover, replacePolicyViewModel.state.primaryApprover)
        assertEquals(alternateApprover, replacePolicyViewModel.state.alternateApprover)
    }

    /**
     * Asserts to expect during user start for a user ready to complete their policy replacement
     *
     * Expected states to assert for:
     *
     * - OwnerRepository.retrieveUser is called
     * - KeyRepository.userHasKeySavedInCloud is called
     * - Owner approver data from policySetup is set to state
     * - OwnerState data is set to global ownerStateFlow
     */
    private suspend fun assertExpectedStatesDuringUserStart() {
        //Verify that global ownerStateFlow was emitted to
        Mockito.verify(ownerRepository, atLeastOnce()).updateOwnerState(any())

        //Assert that approver data was set to state
        assertConfirmedProspectApproverDataSetToState()

        //Verify that userHasKeySavedInCloud is called
        Mockito.verify(keyRepository, times(1)).userHasKeySavedInCloud(genericParticipantId.value)

        //Assert that key download flow was hit since no key data is in state
        //assertKeyDownloadSetToState()
    }
    //endregion

}