package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.home.domain.model.CurationItem
import com.example.artrinx.feature.home.domain.model.MockHomeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class CurationDetailUiState(
    val curation: CurationItem? = null,
    val moreLikeThis: List<CurationItem> = emptyList(),
    val likeCount: Int = 26,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
)

@HiltViewModel
class CurationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val curationId: String = checkNotNull(savedStateHandle["curationId"])

    private val _uiState = MutableStateFlow(buildState(curationId))
    val uiState: StateFlow<CurationDetailUiState> = _uiState.asStateFlow()

    fun onLikeToggled() {
        _uiState.update { state ->
            state.copy(
                isLiked   = !state.isLiked,
                likeCount = if (state.isLiked) state.likeCount - 1 else state.likeCount + 1,
            )
        }
    }

    fun onBookmarkToggled() {
        _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
    }

    companion object {
        private fun buildState(curationId: String): CurationDetailUiState {
            val curation = MockHomeData.popularCurations.find { it.id == curationId }
                ?: MockHomeData.popularCurations.firstOrNull()
            val moreLikeThis = MockHomeData.popularCurations.filter { it.id != curationId }
            return CurationDetailUiState(
                curation    = curation,
                moreLikeThis = moreLikeThis,
                likeCount   = 26,
            )
        }
    }
}
