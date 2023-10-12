package co.censo.vault.presentation.home

sealed class Screen(val route: String) {
    object OwnerEntrance : Screen("owner_entrance_screen")
    object AddBIP39Route : Screen("add_bip39_screen") {
        const val MASTER_PUBLIC_KEY_NAME_ARG = "master_public_key"
    }

    object BIP39DetailRoute : Screen("bip_39_detail_screen") {
        const val BIP_39_NAME_ARG = "bip39_name"
    }
    object FacetecAuthRoute : Screen("facetec_auth_screen")
    object PlanSetupRoute : Screen("plan_setup_route") {
        const val EXISTING_SECURITY_PLAN_ARG = "existing_security_plan"
    }

    object InitialPlanSetupRoute : Screen("initial_plan_setup_route")

    object ActivateApprovers : Screen("activate_approvers_screen")

    object VaultScreen : Screen("vault_screen")

    object RecoveryScreen : Screen("recovery_screen")

    companion object {
        const val START_DESTINATION_ID = 0
    }
}