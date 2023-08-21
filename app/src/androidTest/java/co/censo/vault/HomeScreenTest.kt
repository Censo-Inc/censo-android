package co.censo.vault

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.censo.vault.screen.ComposeAddBIP39Screen
import co.censo.vault.screen.ComposeHomeScreen
import co.censo.vault.util.TestTag
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.components.composesupport.interceptors.behavior.impl.systemsafety.SystemDialogSafetySemanticsBehaviorInterceptor
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.params.FlakySafetyParams
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest : TestCase(
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
    fun testWeAreOnHomeScreen() = run {
        step("Assert user is on Home Screen") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                homeAppBar {
                    assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun testWeCanNavigateToAddBip39() = run {
        step("Assert user is on Home Screen") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                homeAppBar {
                    assertIsDisplayed()
                }
            }
        }

        step("Click on add bip 39 button") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                addBip39Button.performClick()
            }
        }

        step("Check we are on add bip 39 screen") {
            ComposeScreen.onComposeScreen<ComposeAddBIP39Screen>(composeTestRule) {
                addBip39AppBar {
                    assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun canAddBip39Phrase() = run {
        step("Assert user is on Home Screen") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                homeAppBar {
                    assertIsDisplayed()
                }
            }
        }

        step("Click on add bip 39 button") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                addBip39Button.performClick()
            }
        }

        step("Add bip 39 phrase") {
            ComposeScreen.onComposeScreen<ComposeAddBIP39Screen>(composeTestRule) {
                addBip39AppBar {
                    assertIsDisplayed()
                }

                nameTextField.performTextInput("test1")

                phraseTextField.performTextInput("market talent corn beef party situate domain guitar toast system tribe meat provide tennis believe coconut joy salon guide choose few obscure inflict horse")

                saveButton.performClick()
            }
        }

        step("Check we are on home screen with the added phrase") {
            ComposeScreen.onComposeScreen<ComposeHomeScreen>(composeTestRule) {
                homeAppBar {
                    assertIsDisplayed()
                }

                phrasesList {
                    assertIsDisplayed()
                }

                composeTestRule
                    .onNodeWithTag(TestTag.phrases_list)
                    .onChildren()
                    .onFirst()
                    .assert(
                        hasText("test1")
                    )

            }
        }
    }
}