package co.censo.vault

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.components.composesupport.interceptors.behavior.impl.systemsafety.SystemDialogSafetySemanticsBehaviorInterceptor
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.params.FlakySafetyParams
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SavedPhraseAppAccessTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport(
        customize = {
            flakySafetyParams = FlakySafetyParams.custom(timeoutMs = 5000, intervalMs = 1000)
        },
        lateComposeCustomize = { composeBuilder ->
            composeBuilder.semanticsBehaviorInterceptors =
                composeBuilder.semanticsBehaviorInterceptors.filter {
                    it !is SystemDialogSafetySemanticsBehaviorInterceptor
                }.toMutableList()
        }
    )
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun test() = run {
        step("Assert fully authenticated logged in user is prompted for biometry on launch") {
            ComposeScreen.onComposeScreen<ComposeMainActivity>(composeTestRule) {
                foregroundBlockingUI {
                    assertIsDisplayed()
                    assertIsNotFocused()
                }

                val result = adbServer.performAdb("-e emu finger touch 1")
                assert(result[0].contains("exitCode=0, message=OK"))
            }
        }

        step("Assert biometry auth did not fail and retry button does not exist") {
            ComposeScreen.onComposeScreen<ComposeMainActivity>(composeTestRule) {
                foregroundBlockingUI {
                    assertDoesNotExist()
                }
            }
        }
    }
}