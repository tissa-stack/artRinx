package com.rinx.artRINXapp.feature.profile.presentation.posttutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.tour.TourManager
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the one-time Plans screen shown right after the first-launch app tutorial. Resolves the
 * user's role (profile title) so the right plans are listed, and clears the tour's `plansPending`
 * flag when the user taps Continue (which lets Home ask for notification permission next).
 */
@HiltViewModel
class PostTutorialPlansViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val tourManager: TourManager,
) : ViewModel() {

    // Seed from the SWR cache (the tour visits the Profile tab, so it's usually warm) and refresh.
    private val _roleName = MutableStateFlow(repository.cachedProfileData()?.role.orEmpty())
    val roleName: StateFlow<String> = _roleName.asStateFlow()

    init {
        viewModelScope.launch {
            val role = (repository.getProfileData() as? ApiResult.Success)?.data?.role
            if (!role.isNullOrBlank()) _roleName.value = role
        }
    }

    /** Dismiss the finale so Home proceeds to the notification-permission prompt. */
    fun onContinue() = tourManager.markPlansShown()
}
