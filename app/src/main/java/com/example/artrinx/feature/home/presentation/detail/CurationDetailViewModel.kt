package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.domain.model.CurationItem
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
data class CurationDetailUiState(
    val curation: CurationItem? = null,
    val moreLikeThis: List<CurationItem> = emptyList(),
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLoading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class CurationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
) : ViewModel() {

    private val curationId: Int? = savedStateHandle.get<String>("curationId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(CurationDetailUiState())
    val uiState: StateFlow<CurationDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val id = curationId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getCurationDetail(id) }
            val moreJob = async { repository.getMoreCurations() }
            val detailRes = detailJob.await()
            val moreRes = moreJob.await()

            if (detailRes is ApiResult.Success) {
                val curation = detailRes.data
                val more = (moreRes as? ApiResult.Success)?.data.orEmpty()
                    .filter { it.id != curation.id }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = false,
                        curation = curation,
                        moreLikeThis = more,
                        likeCount = curation.likeCount,
                        isLiked = curation.isLiked,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun onLikeToggled() {
        val id = curationId ?: return
        val nowLiked = !_uiState.value.isLiked
        setLiked(nowLiked)
        viewModelScope.launch {
            val result = if (nowLiked) repository.likeCuration(id) else repository.unlikeCuration(id)
            if (result is ApiResult.Error) setLiked(!nowLiked)   // revert on failure
        }
    }

    private fun setLiked(liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLiked = liked,
                likeCount = (state.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
            )
        }
    }
}
