package com.rinx.artRINXapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the logged-in user's numeric id so the `user_profile/{userId}` route can decide whether
 * the opened profile is the current user's own (→ own-profile layout) or someone else's
 * (→ OtherProfileScreen). Seeds synchronously from the repo cache (usually already warm) and
 * refreshes via getMyProfile only when it isn't.
 */
@HiltViewModel
class ProfileRouteViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _currentUserId = MutableStateFlow(profileRepository.cachedCurrentUserId())
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        if (_currentUserId.value == null) {
            viewModelScope.launch {
                (profileRepository.getMyProfile() as? ApiResult.Success)?.let {
                    _currentUserId.value = it.data.id
                }
            }
        }
    }
}
