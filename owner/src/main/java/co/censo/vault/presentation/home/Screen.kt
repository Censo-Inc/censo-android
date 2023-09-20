package co.censo.vault.presentation.home

sealed class Screen(val route: String) {
    object OwnerEntrance : Screen("owner_entrance_screen")
    object AddBIP39Route : Screen("add_bip39_screen")
    object BIP39DetailRoute : Screen("bip_39_detail_screen") {
        const val BIP_39_NAME_ARG = "bip39_name"
    }
    object FacetecAuthRoute : Screen("facetec_auth_screen")
    object GuardianInvitationRoute : Screen("guardian_invitation_screen")

    companion object {
        const val START_DESTINATION_ID = 0

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"

        const val DL_DEVICE_PUBLIC_KEY_KEY = "device_public_key"
        const val DL_INTERMEDIATE_KEY_KEY = "intermediate_key"
        const val DL_PARTICIPANT_ID_KEY = "participant_id"
    }
}