package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.home.domain.model.ArtworkItem
import com.example.artrinx.feature.home.domain.model.MockHomeData
import com.example.artrinx.feature.home.domain.model.ShoppablePost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class ArtDetailUiState(
    val post: ShoppablePost? = null,
    val moreLikeThis: List<ArtworkItem> = emptyList(),
)

@HiltViewModel
class ArtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(buildState(postId))
    val uiState: StateFlow<ArtDetailUiState> = _uiState.asStateFlow()

    fun onLikeToggled() {
        _uiState.update { state ->
            val p = state.post ?: return@update state
            state.copy(
                post = p.copy(
                    isLiked = !p.isLiked,
                    likeCount = if (p.isLiked) p.likeCount - 1 else p.likeCount + 1,
                ),
            )
        }
    }

    companion object {
        private fun buildState(postId: String): ArtDetailUiState {
            val moreLikeThis = MockHomeData.newArtItems

            // 1. Look in shoppable posts
            MockHomeData.shopItems.find { it.id == postId }?.let {
                return ArtDetailUiState(post = it, moreLikeThis = moreLikeThis)
            }

            // 2. Convert feed post → ShoppablePost with default style/description
            MockHomeData.feedItems.find { it.id == postId }?.let { feed ->
                val converted = ShoppablePost(
                    id = feed.id,
                    artistName = feed.artistName,
                    artistHandle = feed.artistHandle,
                    artistRole = feed.artistRole,
                    artistAvatarUrl = feed.artistAvatarUrl,
                    imageUrl = feed.imageUrl,
                    title = feed.title,
                    medium = "Painting",
                    description = "A remarkable piece by ${feed.artistName}.",
                    likeCount = feed.likeCount,
                    commentCount = feed.commentCount,
                    isLiked = feed.isLiked,
                )
                return ArtDetailUiState(post = converted, moreLikeThis = moreLikeThis)
            }

            // 3. Look in artwork items (New Art For You / Recently Viewed / More like this)
            val artItem = (MockHomeData.newArtItems + MockHomeData.recentlyViewed)
                .find { it.id == postId }
            if (artItem != null) {
                val converted = ShoppablePost(
                    id = artItem.id,
                    artistName = artItem.artistName,
                    artistHandle = "@${artItem.artistName.lowercase().replace(" ", "")}",
                    artistRole = "Artist",
                    artistAvatarUrl = artItem.artistAvatarUrl,
                    imageUrl = artItem.imageUrl,
                    title = artItem.title,
                    medium = "Painting",
                    description = "A captivating artwork by ${artItem.artistName}.",
                )
                return ArtDetailUiState(post = converted, moreLikeThis = moreLikeThis)
            }

            // 4. Look in banner (carousel / sponsored) items
            val bannerItem = MockHomeData.bannerItems.find { it.id == postId }
            if (bannerItem != null) {
                val artistName = bannerItem.artistName.removePrefix("by ").trim()
                val converted = ShoppablePost(
                    id = bannerItem.id,
                    artistName = artistName,
                    artistHandle = "@${artistName.lowercase().replace(" ", "")}",
                    artistRole = "Artist",
                    imageUrl = bannerItem.imageUrl,
                    title = bannerItem.title,
                    medium = "Digital Art",
                    description = "A featured sponsored artwork on ArtRinx.",
                )
                return ArtDetailUiState(post = converted, moreLikeThis = moreLikeThis)
            }

            // 5. Fallback to first shop item
            return ArtDetailUiState(
                post = MockHomeData.shopItems.firstOrNull(),
                moreLikeThis = moreLikeThis,
            )
        }
    }
}
