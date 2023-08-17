package co.censo.vault

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import co.censo.vault.screen.ComposeAddBIP39Screen
import co.censo.vault.screen.ComposeBip39DetailScreen
import co.censo.vault.screen.ComposeHomeScreen
import co.censo.vault.screen.ComposeMainActivity
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
    fun `create phrase then leave app and re-enter app and complete biometry`() = run {
        //region create bip 39 phrase
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
        //endregion

        //region background app + re-open app
        UiDevice.getInstance(getInstrumentation())?.apply {
            pressHome()
            pressRecentApps()
            pressRecentApps()
        }
        //endregion

        //region do biometry auth to get in the app
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
        //endregion
    }

    @Test
    fun `must complete biometry to view bip 39 detail phrase`() = run {
        //region create bip 39 phrase
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
        //endregion

        //region background app + re-open app
        UiDevice.getInstance(getInstrumentation())?.apply {
            pressHome()
            pressRecentApps()
            pressRecentApps()
        }
        //endregion

        //region do biometry auth to get in the app
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

        step("Navigate to bip 39 detail") {
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
                    .performClick()
            }
        }

        step("Confirm we are on bip 39 detail") {
            ComposeScreen.onComposeScreen<ComposeBip39DetailScreen>(composeTestRule) {
                bip39DetailAppBar {
                    assertIsDisplayed()
                }

                bip39DetailBiometryText {
                    assertIsDisplayed()
                }
            }
        }

        step("Confirm we are on bip 39 detail") {
            ComposeScreen.onComposeScreen<ComposeBip39DetailScreen>(composeTestRule) {
                bip39DetailAppBar {
                    assertIsDisplayed()
                }

                val result = adbServer.performAdb("-e emu finger touch 1")
                assert(result[0].contains("exitCode=0, message=OK"))

                bip39PhraseUI {
                    assertIsDisplayed()
                }
            }
        }
        //endregion
    }
}