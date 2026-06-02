package com.example.artrinx.feature.profile.presentation.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.feature.profile.domain.model.MockUserProfileData
import com.example.artrinx.feature.profile.domain.model.ProfileTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState(isLoading = true))
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            delay(1500L)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    profile = MockUserProfileData.profile,
                    artItems = MockUserProfileData.artItems,
                    curations = MockUserProfileData.curations,
                    likedItems = MockUserProfileData.likedItems,
                )
            }
        }
    }

    fun onTabSelected(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onBioExpandToggle() {
        _uiState.update { it.copy(isBioExpanded = !it.isBioExpanded) }
    }
}