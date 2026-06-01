package com.example.artrinx.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.feature.home.domain.model.MockHomeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            delay(1500L)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = null,
                    bannerItems = MockHomeData.bannerItems,
                    newArtItems = MockHomeData.newArtItems,
                    popularCurations = MockHomeData.popularCurations,
                    recentlyViewed = MockHomeData.recentlyViewed,
                    feedItems = MockHomeData.feedItems,
                )
            }
        }
    }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onRetry() {
        _uiState.value = HomeUiState(isLoading = true)
        load()
    }

    fun onLikeToggled(postId: String) {
        _uiState.update { state ->
            state.copy(
                feedItems = state.feedItems.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            isLiked = !post.isLiked,
                            likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1,
                        )
                    } else post
                },
            )
        }
    }

    fun onBookmarkToggled(postId: String) {
        _uiState.update { state ->
            state.copy(
                feedItems = state.feedItems.map { post ->
                    if (post.id == postId) post.copy(isBookmarked = !post.isBookmarked) else post
                },
            )
        }
    }
}
