package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.domain.model.ArtworkItem
import com.example.artrinx.feature.home.domain.model.ShoppablePost
import com.example.artrinx.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ArtDetailUiState(
    val post: ShoppablePost? = null,
    val moreLikeThis: List<ArtworkItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class ArtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
) : ViewModel() {

    private val artworkId: Int? = savedStateHandle.get<String>("postId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(ArtDetailUiState())
    val uiState: StateFlow<ArtDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val id = artworkId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getArtworkDetail(id) }
            val similarJob = async { repository.getSimilarArtworks(id) }
            val detailRes = detailJob.await()
            val similarRes = similarJob.await()

            if (detailRes is ApiResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = false,
                        post = detailRes.data,
                        moreLikeThis = (similarRes as? ApiResult.Success)?.data.orEmpty(),
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun onLikeToggled() {
        val id = artworkId ?: return
        val post = _uiState.value.post ?: return
        val nowLiked = !post.isLiked
        setLiked(nowLiked)
        viewModelScope.launch {
            val result = if (nowLiked) repository.likeArtwork(id) else repository.unlikeArtwork(id)
            if (result is ApiResult.Error) setLiked(!nowLiked)   // revert on failure
        }
    }

    private fun setLiked(liked: Boolean) {
        _uiState.update { state ->
            val p = state.post ?: return@update state
            state.copy(
                post = p.copy(
                    isLiked = liked,
                    likeCount = (p.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
                ),
            )
        }
    }
}
