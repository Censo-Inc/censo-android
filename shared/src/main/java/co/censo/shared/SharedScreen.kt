package co.censo.shared


sealed class SharedScreen(val route: String) {
    object EntranceRoute : SharedScreen("entrance_screen")
    object OwnerRoutingScreen : SharedScreen("owner_routing_screen")
    companion object {
        const val APPROVER_INVITE_URI = "${BuildConfig.URL_SCHEME}://invite/"
        const val APPROVER_ACCESS_URI = "${BuildConfig.URL_SCHEME}://access/"
    }
}