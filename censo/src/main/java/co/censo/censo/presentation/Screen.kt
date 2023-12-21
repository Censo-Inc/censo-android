package co.censo.censo.presentation

import Base58EncodedMasterPublicKey
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.shared.data.model.AccessIntent

sealed class Screen(val route: String) {
    object EntranceRoute : Screen("entrance_screen")

    object PolicySetupRoute : Screen("policy_setup_route") {
        const val SETUP_ACTION_ARG = "setup_action_key"

        fun addApproversRoute(): String = "${PolicySetupRoute.route}/${PolicySetupAction.AddApprovers.name}"
        fun removeApproversRoute(): String = "${PolicySetupRoute.route}/${PolicySetupAction.RemoveApprovers.name}"
    }

    object ReplacePolicyRoute: Screen("replace_policy_route") {
        const val REPLACE_POLICY_ACTION_ARG = "replace_policy_action_key"

        private fun addApproversRoute(): String = "${ReplacePolicyRoute.route}/${PolicySetupAction.AddApprovers.name}"
        private fun removeApproversRoute(): String = "${ReplacePolicyRoute.route}/${PolicySetupAction.RemoveApprovers.name}"

        fun buildNavRoute(addApprovers: Boolean) : String = if (addApprovers) addApproversRoute() else removeApproversRoute()
    }

    object OwnerVaultScreen : Screen("owner_vault_screen")

    object InitialPlanSetupRoute : Screen("initial_plan_setup_route")

    object AccessSeedPhrases : Screen("access_seed_phrases")

    object AccessApproval : Screen("access_approval") {
        const val ACCESS_INTENT_ARG = "access_intent_key"

        fun withIntent(intent: AccessIntent): String {
            return "${AccessApproval.route}/${intent.name}"
        }
    }

    object OwnerKeyRecoveryRoute : Screen("owner_key_recovery")

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
}