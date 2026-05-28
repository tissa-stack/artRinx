package com.example.artrinx.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteCodeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCodeUiState())
    val uiState: StateFlow<InviteCodeUiState> = _uiState.asStateFlow()

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(inviteCode = code, errorMessage = null) }
    }

    fun onContinue() {
        val code = _uiState.value.inviteCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(800L)
            // TODO: Replace with real API call
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid invitation code") }
        }
    }
}
