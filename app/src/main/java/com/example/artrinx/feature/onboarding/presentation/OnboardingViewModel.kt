package com.example.artrinx.feature.onboarding.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.appDataStore
import com.example.artrinx.feature.onboarding.data.OnboardingRepositoryImpl
import com.example.artrinx.feature.onboarding.domain.usecase.GetOnboardingPagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnboardingRepositoryImpl(application.appDataStore)
    private val getPagesUseCase = GetOnboardingPagesUseCase()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(pages = getPagesUseCase()) }
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun nextPage() {
        val current = _uiState.value
        if (current.currentPage < current.pages.lastIndex) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        } else {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            repository.markOnboardingComplete()
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
