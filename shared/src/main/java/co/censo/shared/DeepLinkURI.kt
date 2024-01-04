package co.censo.shared

import co.censo.shared.CensoLink.Companion.CENSO_HOST
import co.censo.shared.CensoLink.Companion.HOST_INDEX
import co.censo.shared.CensoLink.Companion.LINK_TYPE_INDEX
import co.censo.shared.CensoLink.Companion.TYPES
import co.censo.shared.CensoLink.Companion.V1_PART_ID_INDEX
import co.censo.shared.CensoLink.Companion.V1_SIZE
import co.censo.shared.CensoLink.Companion.V2
import co.censo.shared.CensoLink.Companion.V2_ACCESS_SIZE
import co.censo.shared.CensoLink.Companion.V2_APPROVAL_ID_INDEX
import co.censo.shared.CensoLink.Companion.V2_INDEX
import co.censo.shared.CensoLink.Companion.V2_PART_ID_INDEX


object DeepLinkURI {
    const val APPROVER_INVITE_URI = "${BuildConfig.APPROVER_URL_SCHEME}://invite/"
    const val APPROVER_ACCESS_URI = "${BuildConfig.APPROVER_URL_SCHEME}://access/"
    const val APPROVER_ACCESS_V2_URI = "${BuildConfig.APPROVER_URL_SCHEME}://access/v2/"
    const val OWNER_LOGIN_ID_RESET_URI = "${BuildConfig.OWNER_URL_SCHEME}://reset/"
}

fun String.parseLink(): CensoLink {
    val parts = this.replace(Regex("[\\r\\n]+"), "").trim().split("//")
    if (parts.size != 2 || !parts[HOST_INDEX].startsWith(CENSO_HOST)) {
        throw Exception("invalid link")
    }
    val routeAndIdentifier = parts[1].split("/")

    val type = routeAndIdentifier[LINK_TYPE_INDEX]

    if (type !in TYPES) {
        throw Exception("invalid link")
    }

    if (routeAndIdentifier.size == V2_ACCESS_SIZE && routeAndIdentifier[V2_INDEX] == V2) {
        return CensoLink(
            routeAndIdentifier[HOST_INDEX],
            CensoLink.IdLinks(
                mainId = routeAndIdentifier[V2_PART_ID_INDEX],
                approvalId = routeAndIdentifier[V2_APPROVAL_ID_INDEX]
            )
        )
    }

    if (routeAndIdentifier.size != V1_SIZE) {
        throw Exception("invalid link")
    }
    return CensoLink(
        routeAndIdentifier[HOST_INDEX],
        CensoLink.IdLinks(mainId = routeAndIdentifier[V1_PART_ID_INDEX])
    )
}

data class CensoLink(
    val type: String,
    val identifiers: IdLinks
) {
    companion object {
        //V1 Format: host://type/[participant_id or invitation_id]

        //V2 Format: host://type/v2/participant_id/approval_id

        //Same for Both V1 and V2
        const val HOST_INDEX = 0
        const val LINK_TYPE_INDEX = 0
        const val ACCESS_TYPE = "access"
        const val INVITE_TYPE = "invite"
        const val RESET_TYPE = "reset"
        const val CENSO_HOST = "censo"
        val TYPES = setOf(ACCESS_TYPE, INVITE_TYPE, RESET_TYPE)

        //V1
        const val V1_PART_ID_INDEX = 1
        const val V1_SIZE = 2

        //V2
        const val V2 = "v2"
        const val V2_ACCESS_SIZE = 4
        const val V2_INDEX = 1
        const val V2_PART_ID_INDEX = 2
        const val V2_APPROVAL_ID_INDEX = 3

    }

    data class IdLinks(
        val mainId: String,
        val approvalId: String? = null
    )
}