package com.example.artrinx.feature.profile.presentation.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.model.MockUserProfileData
import com.example.artrinx.feature.profile.domain.model.ProfileArtItem
import com.example.artrinx.feature.profile.domain.model.ProfileCurationItem
import com.example.artrinx.feature.profile.domain.model.ProfileTab
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.upload.domain.CurationManager
import com.example.artrinx.feature.upload.domain.UploadManager
import com.example.artrinx.feature.upload.domain.model.CurationProgress
import com.example.artrinx.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val uploadManager: UploadManager,
    private val curationManager: CurationManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState(isLoading = true))
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        load()
        observeUploads()
        observeCurations()
    }

    private fun load() {
        viewModelScope.launch {
            // Profile header and liked remain mock for this phase; Art + Curations tabs are real.
            val artworksResult = profileRepository.getMyArtworks(PAGE, SIZE)
            val curationsResult = profileRepository.getMyCurations(PAGE, SIZE)
            val artItems = (artworksResult as? ApiResult.Success)?.data ?: MockUserProfileData.artItems
            val curations = (curationsResult as? ApiResult.Success)?.data ?: MockUserProfileData.curations
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    profile = MockUserProfileData.profile,
                    artItems = artItems,
                    curations = curations,
                    likedItems = MockUserProfileData.likedItems,
                )
            }
        }
    }

    // ── Upload progress (PRIVATE uploads only) ─────────────────────────────────

    private fun observeUploads() {
        viewModelScope.launch {
            uploadManager.progress.collect { progress ->
                val forProfile = progress?.takeIf { it.isPrivate }
                _uiState.update { it.copy(uploadProgress = forProfile) }

                if (forProfile is UploadProgress.Success) {
                    _uiState.update { state ->
                        val newItem = forProfile.toProfileArtItem()
                        val deduped = state.artItems.filterNot { it.id == newItem.id }
                        state.copy(
                            artItems = listOf(newItem) + deduped,
                            profile = state.profile?.let { it.copy(artCount = it.artCount + 1) },
                            uploadProgress = null,
                        )
                    }
                    uploadManager.dismiss()
                }
            }
        }
    }

    private fun UploadProgress.Success.toProfileArtItem(): ProfileArtItem = ProfileArtItem(
        id = artwork.id.toString(),
        imageRes = null,
        imageUrl = artwork.imageUrl.ifEmpty { localThumb.toString() },
        title = title,
        artistName = artistName,
        isPrivate = true,
    )

    fun onRetryUpload() = uploadManager.retry()
    fun onDismissUpload() = uploadManager.dismiss()

    // ── Curation progress (PRIVATE curations only) ─────────────────────────────

    private fun observeCurations() {
        viewModelScope.launch {
            curationManager.progress.collect { progress ->
                val forProfile = progress?.takeIf { it.isPrivate }
                _uiState.update { it.copy(curationProgress = forProfile) }

                if (forProfile is CurationProgress.Success) {
                    _uiState.update { state ->
                        val newItem = forProfile.toProfileCurationItem()
                        val deduped = state.curations.filterNot { it.id == newItem.id }
                        state.copy(
                            curations = listOf(newItem) + deduped,
                            profile = state.profile?.let { it.copy(curationCount = it.curationCount + 1) },
                            curationProgress = null,
                        )
                    }
                    curationManager.dismiss()
                }
            }
        }
    }

    private fun CurationProgress.Success.toProfileCurationItem(): ProfileCurationItem = ProfileCurationItem(
        id = curation.id.toString(),
        title = title,
        handle = "",
        artworkRes = emptyList(),
        isPrivate = true,
        artworkUrls = artworkUrls,
    )

    fun onRetryCuration() = curationManager.retry()
    fun onDismissCuration() = curationManager.dismiss()

    fun onTabSelected(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onBioExpandToggle() {
        _uiState.update { it.copy(isBioExpanded = !it.isBioExpanded) }
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 30
    }
}
