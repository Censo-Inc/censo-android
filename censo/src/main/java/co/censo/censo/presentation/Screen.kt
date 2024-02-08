package co.censo.censo.presentation

import Base58EncodedMasterPublicKey
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.util.NavigationData

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
        const val IMPORTING_PHRASE_ARG = "importing_phrase_arg"
        const val ENCRYPTED_PHRASE_ARG = "encrypted_phrase_arg"

        private const val NO_PHRASE = "empty"

        fun buildNavRoute(
            masterPublicKey: Base58EncodedMasterPublicKey,
            welcomeFlow: Boolean,
            importingPhrase: Boolean = false,
            encryptedPhraseData: String = ""
        ): String {
            val phraseData = encryptedPhraseData.ifEmpty { NO_PHRASE }

            return "${EnterPhraseRoute.route}/${masterPublicKey.value}/${welcomeFlow}/${importingPhrase}/${phraseData}"
        }
    }

    data object BeneficiarySignInRoute : Screen("beneficiary_sign_in") {
        const val BENEFICIARY_INVITE_ID = "beneficiary_invite_id"
        fun buildBeneficiaryNavRoute(beneficiaryInviteId: String) =
            "${BeneficiarySignInRoute.route}/${beneficiaryInviteId}"

    }

    data object AcceptBeneficiaryInvitation : Screen("accept_beneficiary_invitation") {
        const val INVITE_ID_ARG = "invite_id_arg"
        const val NO_INVITE_ID = "no_invite_id"

        fun buildNavRoute(
            beneficiaryInviteId: String?,
        ): String {
            return if (beneficiaryInviteId.isNullOrEmpty()) {
                "${AcceptBeneficiaryInvitation.route}/${NO_INVITE_ID}"
            }else {
                "${AcceptBeneficiaryInvitation.route}/${beneficiaryInviteId}"
            }
        }
    }

    data object Beneficiary: Screen("beneficiary_setup")

    object LoginIdResetRoute : Screen("login_id_reset_route") {
        const val DL_RESET_TOKEN_KEY = "reset_token_key"
    }

    data object BiometryResetRoute : Screen("biometry_reset_route")

    fun navTo() : NavigationData {
        return NavigationData(
            route = this.route,
            popSelfFromBackStack = false,
            popUpToTop = false
        )
    }

    fun navToAndPopCurrentDestination() : NavigationData {
        return NavigationData(
            route = this.route,
            popSelfFromBackStack = true,
            popUpToTop = false
        )
    }

    fun String.navTo() : NavigationData {
        return NavigationData(
            route = this,
            popSelfFromBackStack = false,
            popUpToTop = false
        )
    }

    fun String.navToAndPopCurrentDestination() : NavigationData {
        return NavigationData(
            route = this,
            popSelfFromBackStack = true,
            popUpToTop = false
        )
    }

    companion object {
        const val CENSO_IMPORT_DEEPLINK = "censoImportDeepLink"
        const val IMPORT_KEY_KEY = "import_key_key"
        const val TIMESTAMP_KEY = "timestamp_key"
        const val SIGNATURE_KEY = "signature_key"
        const val NAME_KEY = "name_key"
    }
}