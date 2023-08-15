package co.censo.vault.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import co.censo.vault.TestTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ComposeMainActivity(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ComposeMainActivity>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTag.main_activity_surface_container) }
    ) {

    val foregroundBlockingUI: KNode = child {
        hasTestTag(TestTag.biometry_blocking_ui_container)
    }
}