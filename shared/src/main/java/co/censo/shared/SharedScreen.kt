package co.censo.shared

import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError

fun SharedScreen.buildScreenDeepLinkUri() = "${SharedScreen.GUARDIAN_ONBOARDING_URI}${this.route}"

fun String.getInviteCodeFromDeeplink() =
    try {
        split("/").last()
    } catch (e: Exception) {
        e.sendError(CrashReportingUtil.InviteDeeplink)
        ""
    }

sealed class SharedScreen(val route: String) {
    object EntranceRoute : SharedScreen("entrance_screen")
    object OwnerRoutingScreen : SharedScreen("owner_routing_screen")
    object ApproverRoutingScreen : SharedScreen("approver_routing_screen")
    companion object {
        const val GUARDIAN_ONBOARDING_URI = "${BuildConfig.URL_SCHEME}://invite/"
        const val GUARDIAN_RECOVERY_URI = "${BuildConfig.URL_SCHEME}://access/"

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"
        const val DL_INVITATION_ID_KEY = "invitation_id_key"

        const val GUARDIAN_DEEPLINK_RECOVERY = "guardianDeepLinkRecovery"
        const val DL_PARTICIPANT_ID_KEY = "participant_id_key"

    }
}