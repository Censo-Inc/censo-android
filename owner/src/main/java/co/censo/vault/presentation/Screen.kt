package co.censo.vault.presentation

import Base58EncodedMasterPublicKey

sealed class Screen(val route: String) {
    object BIP39DetailRoute : Screen("bip_39_detail_screen") {
        const val BIP_39_NAME_ARG = "bip39_name"
    }

    object PlanSetupRoute : Screen("plan_setup_route") {
        const val WELCOME_FLOW_ARG = "welcome_flow_key"

        fun buildNavRoute(welcomeFlow: Boolean = false): String {
            return "${PlanSetupRoute.route}/${welcomeFlow}"
        }
    }

    object InitialPlanSetupRoute : Screen("initial_plan_setup_route")

    object ActivateApprovers : Screen("activate_approvers_screen")

    object AccessSeedPhrases : Screen("access_seed_phrases")

    object VaultScreen : Screen("vault_screen")

    object AccessApproval : Screen("access_approval")

    object EnterPhraseRoute : Screen("enter_phrase_screen") {
        const val MASTER_PUBLIC_KEY_NAME_ARG = "master_public_key"
        const val WELCOME_FLOW_ARG = "welcome_flow_key"

        fun buildNavRoute(
            masterPublicKey: Base58EncodedMasterPublicKey,
            welcomeFlow: Boolean
        ): String {
            return "${EnterPhraseRoute.route}/${masterPublicKey.value}/${welcomeFlow}"
        }
    }

    companion object {
        const val START_DESTINATION_ID = 0
    }
}