package com.rinx.artRINXapp.feature.profile.presentation.view

import androidx.compose.runtime.Immutable
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.domain.model.UserProfileData
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress

@Immutable
data class UserProfileUiState(
    val profile: UserProfileData? = null,
    val activeTab: ProfileTab = ProfileTab.ART,
    val artItems: List<ProfileArtItem> = emptyList(),
    val curations: List<ProfileCurationItem> = emptyList(),
    val likedItems: List<ProfileArtItem> = emptyList(),
    /** In-progress/just-finished PRIVATE upload, surfaced at the top of the Art tab. */
    val uploadProgress: UploadProgress? = null,
    /** In-progress/just-finished PRIVATE curation, surfaced at the top of the Curations tab. */
    val curationProgress: CurationProgress? = null,
    val isBioExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)