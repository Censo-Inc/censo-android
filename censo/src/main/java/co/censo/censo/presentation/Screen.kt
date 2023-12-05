package co.censo.censo.presentation

import Base58EncodedMasterPublicKey
import co.censo.censo.presentation.plan_setup.PlanSetupDirection
import co.censo.shared.data.model.RecoveryIntent

sealed class Screen(val route: String) {
    object EntranceRoute : Screen("entrance_screen")

    object PlanSetupRoute : Screen("plan_setup_route") {
        const val SETUP_DIRECTION_ARG = "setup_direction_key"

        fun addApproversRoute(): String = "${PlanSetupRoute.route}/${PlanSetupDirection.AddApprovers.name}"
        fun removeApproversRoute(): String = "${PlanSetupRoute.route}/${PlanSetupDirection.RemoveApprovers.name}"
    }

    object OwnerVaultScreen : Screen("owner_vault_screen")

    object OwnerWelcomeScreen : Screen("owner_welcome_screen")

    object InitialPlanSetupRoute : Screen("initial_plan_setup_route")

    object AccessSeedPhrases : Screen("access_seed_phrases")

    object AccessApproval : Screen("access_approval") {
        const val ACCESS_INTENT_ARG = "access_intent_key"

        fun withIntent(
            intent: RecoveryIntent,
        ): String {
            return "${AccessApproval.route}/${intent.name}"
        }
    }

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