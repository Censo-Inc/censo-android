package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LabelOwnerByApproverApiRequest(
    val label: String,
)
