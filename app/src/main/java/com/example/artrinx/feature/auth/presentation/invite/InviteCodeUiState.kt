package com.example.artrinx.feature.auth.presentation.invite

data class InviteCodeUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

val InviteCodeUiState.isSubmitEnabled: Boolean
    get() = inviteCode.isNotBlank() && !isLoading
