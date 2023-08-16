package co.censo.vault.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import co.censo.vault.TestTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ComposeBip39DetailScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ComposeBip39DetailScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTag.bip_39_detail_screen_container) }
    ) {

    val bip39DetailAppBar: KNode = child {
        hasTestTag(TestTag.bip_39_detail_screen_app_bar)
    }

    val bip39DetailBiometryText: KNode = child {
        hasTestTag(TestTag.bip_39_detail_biometry_text)
    }

    val bip39PhraseUI : KNode = child {
        hasTestTag(TestTag.bip_39_detail_phrase_ui)
    }
}