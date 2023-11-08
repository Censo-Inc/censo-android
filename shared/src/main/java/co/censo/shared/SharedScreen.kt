package co.censo.shared


sealed class SharedScreen(val route: String) {
    companion object {
        const val APPROVER_INVITE_URI = "${BuildConfig.URL_SCHEME}://invite/"
        const val APPROVER_ACCESS_URI = "${BuildConfig.URL_SCHEME}://access/"
    }
}