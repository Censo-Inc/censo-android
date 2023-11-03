package co.censo.censo

import org.junit.Before
import org.mockito.MockitoAnnotations

open class BaseViewModelTest {

    @Before
    open fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

}