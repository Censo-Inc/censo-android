package co.censo.censo

import co.censo.censo.BaseViewModelTest
import co.censo.censo.test_helper.genericParticipantId
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerArgs
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerState
import co.censo.shared.presentation.cloud_storage.CloudStorageHandlerViewModel
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import junit.framework.TestCase.assertEquals
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

    private val mockParticipantId = genericParticipantId

    private val mockEncryptedPrivateKey = byteArrayOf(1, 0, 1, 0)
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
            participantId = mockParticipantId,
            privateKey = mockEncryptedPrivateKey
        )

        //Assert download action and data was set to state
        val expectedCloudStorageHandlerArgs = CloudStorageHandlerArgs(
            participantId = mockParticipantId, encryptedPrivateKey = mockEncryptedPrivateKey
        )
        assertEquals(expectedCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(downloadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)


        //Call onDispose to reset state
        cloudStorageHandlerViewModel.onDispose()

        assertDefaultVMState()

        //Call onStart with uploadAction
        cloudStorageHandlerViewModel.onStart(
            actionToPerform = uploadAction,
            participantId = mockParticipantId,
            privateKey = mockEncryptedPrivateKey
        )

        //Assert upload action and data was set to state
        assertEquals(expectedCloudStorageHandlerArgs, cloudStorageHandlerViewModel.state.cloudStorageHandlerArgs)
        assertEquals(uploadAction, cloudStorageHandlerViewModel.state.cloudActionToPerform)
    }

    @Test
    fun `test perform action with UNINITIALIZED state does not trigger any repository methods`() = runTest {
        assertDefaultVMState()

        //Cloud action in state is UNINITIALIZED by default, so just call performAction
        cloudStorageHandlerViewModel.performAction()

        testScheduler.advanceUntilIdle()

        Mockito.verify(keyRepository, never()).retrieveKeyFromCloud(participantId = mockParticipantId)
        Mockito.verify(keyRepository, never()).saveKeyInCloud(key = mockEncryptedPrivateKey, participantId = mockParticipantId)
    }

    //TODO:
    //Enforce access action
    // download/upload actions


    //endregion

    //region Flow tests
    //endregion

    //region Custom asserts
    private fun assertDefaultVMState() {
        assertEquals(CloudStorageHandlerState(), cloudStorageHandlerViewModel.state)
    }
    //endregion

    //region Helper methods
    //endregion
}