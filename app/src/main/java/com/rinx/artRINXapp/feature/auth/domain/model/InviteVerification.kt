package com.rinx.artRINXapp.feature.auth.domain.model

/**
 * Outcome of POST /verify-invite. The backend disambiguates the single code field into a
 * [InviteCodeType]; the UI branches on it (peer/admin → signup, agent → "complete on web").
 */
data class InviteVerification(
    val code: String,
    val codeType: InviteCodeType,
    val remainingInvites: Int? = null,
)

enum class InviteCodeType {
    PEER,
    ADMIN,
    AGENT,
    /** code_type absent/unrecognized (legacy backend) — treat as a normal peer signup. */
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): InviteCodeType = when (raw?.trim()?.lowercase()) {
            "peer" -> PEER
            "admin" -> ADMIN
            "agent" -> AGENT
            else -> UNKNOWN
        }
    }
}
