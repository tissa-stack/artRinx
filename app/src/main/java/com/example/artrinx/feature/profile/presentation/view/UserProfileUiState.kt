package com.example.artrinx.feature.profile.presentation.view

import androidx.compose.runtime.Immutable
import com.example.artrinx.feature.profile.domain.model.ProfileArtItem
import com.example.artrinx.feature.profile.domain.model.ProfileCurationItem
import com.example.artrinx.feature.profile.domain.model.ProfileTab
import com.example.artrinx.feature.profile.domain.model.UserProfileData

@Immutable
data class UserProfileUiState(
    val profile: UserProfileData? = null,
    val activeTab: ProfileTab = ProfileTab.ART,
    val artItems: List<ProfileArtItem> = emptyList(),
    val curations: List<ProfileCurationItem> = emptyList(),
    val likedItems: List<ProfileArtItem> = emptyList(),
    val isBioExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)