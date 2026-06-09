package com.rinx.artRINXapp.feature.create.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.UploadQuota
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Create tab's "Upload Art" gate (handout §Upload tap handler). Loads the upload quota on
 * entry and resolves the role×plan branch when the user taps Upload Art.
 */
@HiltViewModel
class CreateViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    /** The handout's four upload outcomes, plus a transient Loading. */
    enum class UploadAction { OPEN_PICKER, LIMIT_REACHED, GALLERY_WEB, UPGRADE_REQUIRED, LOADING }

    data class State(
        val quota: UploadQuota? = null,
        val uploadLimitText: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val result = profileRepository.getUploadQuota()
            if (result is ApiResult.Success) {
                val q = result.data
                _state.update {
                    it.copy(quota = q, uploadLimitText = "${q.artworkCount} / ${q.maxUploads} uploads")
                }
            }
        }
    }

    /** Resolve the Upload-Art tap branch. Degrades to OPEN_PICKER when the quota hasn't loaded
     * (the backend re-checks on POST, so client gating is UX-only — never a hard security gate). */
    fun resolveUploadAction(): UploadAction {
        val q = _state.value.quota ?: return UploadAction.OPEN_PICKER
        return when {
            q.isPaid && q.limitReached -> UploadAction.LIMIT_REACHED
            q.isPaid -> UploadAction.OPEN_PICKER
            q.isGallery -> UploadAction.GALLERY_WEB
            else -> UploadAction.UPGRADE_REQUIRED
        }
    }
}
