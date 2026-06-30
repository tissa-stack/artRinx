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
    /** True while a manual pull-to-refresh is in flight (drives the refresh spinner). */
    val isRefreshing: Boolean = false,
    /** Full-screen error message shown only on a cold load failure (no profile to display). */
    val error: String? = null,
    /** True when [error] is due to the device being offline → drives the "No internet" error state
     *  (vs. a generic error). Real connectivity, so a no-connection message never shows while online. */
    val isOffline: Boolean = false,
    /** One-shot message when a manual pull-to-refresh fails while content is already shown; cleared via
     *  [UserProfileViewModel.consumeRefreshError] after the screen toasts it. */
    val refreshError: String? = null,
    // ── Pagination (per tab) ──
    val isLoadingMore: Boolean = false,
    val artHasMore: Boolean = false,
    val curationHasMore: Boolean = false,
    val likedHasMore: Boolean = false,
)