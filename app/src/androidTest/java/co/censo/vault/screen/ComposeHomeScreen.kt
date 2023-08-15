package co.censo.vault.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import co.censo.vault.TestTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ComposeHomeScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ComposeHomeScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTag.home_screen_container) }
    ) {

    val homeAppBar: KNode = child {
        hasTestTag(TestTag.home_screen_app_bar)
    }

    val addBip39Button: KNode = child {
        hasTestTag(TestTag.add_bip39_button)
    }

    val phrasesList: KNode = child {
        hasTestTag(TestTag.phrases_list)
    }

    val phraseRowItem: KNode = child {
        hasTestTag(TestTag.phrase_row_item)
    }
}