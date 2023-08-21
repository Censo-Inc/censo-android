package co.censo.vault.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import co.censo.vault.util.TestTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ComposeAddBIP39Screen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ComposeAddBIP39Screen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTag.add_bip_39_screen_container) }
    ) {

    val addBip39AppBar: KNode = child {
        hasTestTag(TestTag.add_bip_39_screen_app_bar)
    }

    val nameTextField: KNode = child {
        hasTestTag(TestTag.add_bip_39_name_text_field)
    }

    val phraseTextField: KNode = child {
        hasTestTag(TestTag.add_bip_39_phrase_text_field)
    }

    val saveButton: KNode = child {
        hasTestTag(TestTag.add_bip_39_save_button)
    }
}