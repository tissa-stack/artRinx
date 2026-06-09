package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * POST /verify-invite response: {data: {code_type, inviter_id?, remaining_invites?, month}}.
 * All fields optional so a legacy/empty body decodes without throwing.
 */
data class VerifyInviteResponse(
    @SerializedName("data") val data: VerifyInviteData? = null,
)

data class VerifyInviteData(
    @SerializedName("code_type") val codeType: String? = null,
    @SerializedName("inviter_id") val inviterId: Long? = null,
    @SerializedName("remaining_invites") val remainingInvites: Int? = null,
    @SerializedName("month") val month: String? = null,
)
