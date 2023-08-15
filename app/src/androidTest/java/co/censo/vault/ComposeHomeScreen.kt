package co.censo.vault

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ComposeHomeScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ComposeHomeScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTag.home_screen_container) }
    ) {

    val simpleHomeAppBar: KNode = child {
        hasTestTag(TestTag.home_screen_app_bar)
    }
}