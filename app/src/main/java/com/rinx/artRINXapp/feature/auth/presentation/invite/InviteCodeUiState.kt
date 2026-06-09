package com.rinx.artRINXapp.feature.auth.presentation.invite

data class InviteCodeUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set for peer/admin codes → navigate to signup with [inviteCode]. */
    val isSuccess: Boolean = false,
    /** Set for agent (Gallery) codes → show the "complete on web" modal instead of signup. */
    val showGalleryWebModal: Boolean = false,
)

val InviteCodeUiState.isSubmitEnabled: Boolean
    get() = inviteCode.isNotBlank() && !isLoading
