package co.censo.censo.view_model_tests

import co.censo.censo.test_helper.genericParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerArgs
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerState
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerViewModel
import com.nhaarman.mockitokotlin2.atLeastOnce
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
import org.mockito.Mock
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class CloudStorageHandlerViewModelTest : BaseViewModelTest() {

    //region Mocks and test objects
    @Mock
    lateinit var keyRepository: KeyRepository

    private lateinit var cloudStorageHandlerViewModel: CloudStorageHandlerViewModel

    private val testDispatcher = StandardTestDispatcher()
    //endregion

    //region test data
    private val downloadAction = CloudStorageActions.DOWNLOAD
    private val uploadAction = CloudStorageActions.UPLOAD
    private val enforceAccessAction = CloudStorageActions.ENFORCE_ACCESS

    private val mockParticipantId = genericParticipantId

    private val mockEncryptedPrivateKey = byteArrayOf(1, 0, 1, 0)

    private val expectedMockCloudStorageHandlerArgs = CloudStorageHandlerArgs(
        id = mockParticipantId.value, encryptedPrivateKey = mockEncryptedPrivateKey
    )
    //endregion

    //region setup tear down
    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        cloudStorageHandlerViewModel = CloudStorageHandlerViewModel(
            keyRepository = keyRepository, ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //endregion

    //region Focused tests
    @Test
    fun `call onStart then VM should set cloud action data to state`() {
        assertDefaultVMState()

        cloudStorageHandlerViewModel.onStart(
            actionToPerform = downloadAction,
            id = mockParticipantId.value,
            privateKey = mockEncryptedPrivateKey
        )

        //Assert download action and data was set to state
        assertEquals(expectedMockCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(downloadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)


        //Call onDispose to reset state
        cloudStorageHandlerViewModel.onDispose()

        assertDefaultVMState()

        //Call onStart with uploadAction
        cloudStorageHandlerViewModel.onStart(
            actionToPerform = uploadAction,
            id = mockParticipantId.value,
            privateKey = mockEncryptedPrivateKey
        )

        //Assert upload action and data was set to state
        assertEquals(expectedMockCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(uploadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)
    }

    @Test
    fun `test perform action with UNINITIALIZED state does not trigger any repository methods`() = runTest {
        assertDefaultVMState()

        //Cloud action in state is UNINITIALIZED by default, so just call performAction
        cloudStorageHandlerViewModel.performAction()

        testScheduler.advanceUntilIdle()

        Mockito.verify(keyRepository, never()).retrieveKeyFromCloud(id = mockParticipantId.value)
        Mockito.verify(keyRepository, never()).saveKeyInCloud(key = mockEncryptedPrivateKey, id = mockParticipantId.value)
    }

    @Test
    fun `call onStart with download action then VM should download key from the cloud`() = runTest {
        assertDefaultVMState()

        //Mock the keyRepository.retrieveKeyFromCloud
        whenever(keyRepository.retrieveKeyFromCloud(genericParticipantId.value)).thenAnswer {
            Resource.Success(mockEncryptedPrivateKey)
        }

        cloudStorageHandlerViewModel.onStart(
            actionToPerform = downloadAction,
            id = mockParticipantId.value,
            privateKey = mockEncryptedPrivateKey
        )

        testScheduler.advanceUntilIdle()

        //Assert download action and data was set to state
        assertEquals(expectedMockCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(downloadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)

        //Verify repo method was called
        Mockito.verify(keyRepository, atLeastOnce()).retrieveKeyFromCloud(genericParticipantId.value)

        //Assert key data was loaded
        assertTrue(cloudStorageHandlerViewModel.state.cloudStorageActionResource is Resource.Success)
        assertEquals(mockEncryptedPrivateKey, cloudStorageHandlerViewModel.state.cloudStorageActionResource.success()?.data)
    }

    @Test
    fun `call onStart with upload action then VM should upload key to the cloud`() = runTest {
        assertDefaultVMState()

        //Mock the keyRepository.saveKeyInCloud
        whenever(keyRepository.saveKeyInCloud(mockEncryptedPrivateKey, genericParticipantId.value)).thenAnswer {
            Resource.Success(Unit)
        }

        cloudStorageHandlerViewModel.onStart(
            actionToPerform = uploadAction,
            id = mockParticipantId.value,
            privateKey = mockEncryptedPrivateKey
        )

        testScheduler.advanceUntilIdle()

        //Assert upload action and data was set to state
        assertEquals(expectedMockCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(uploadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)

        //Verify repo method was called
        Mockito.verify(keyRepository, atLeastOnce()).saveKeyInCloud(mockEncryptedPrivateKey, genericParticipantId.value)

        //Assert key data was saved
        assertTrue(cloudStorageHandlerViewModel.state.cloudStorageActionResource is Resource.Success)
        assertEquals(mockEncryptedPrivateKey, cloudStorageHandlerViewModel.state.cloudStorageActionResource.success()?.data)
    }

    @Test
    fun `call onStart with enforce access action then VM should set state for enforcing cloud access`() = runTest {
        assertDefaultVMState()

        cloudStorageHandlerViewModel.onStart(
            actionToPerform = enforceAccessAction,
            id = mockParticipantId.value,
            privateKey = null
        )

        testScheduler.advanceUntilIdle()

        //Assert upload action and data was set to state
        assertEquals(expectedMockCloudStorageHandlerArgs.copy(encryptedPrivateKey = null), cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(enforceAccessAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)

        assertTrue(cloudStorageHandlerViewModel.state.shouldEnforceCloudStorageAccess)
    }
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(CloudStorageHandlerState(), cloudStorageHandlerViewModel.state)
    }
    //endregion
}