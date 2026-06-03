package com.example.artrinx.feature.home.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.domain.model.HomeFeed
import com.example.artrinx.feature.home.domain.model.ShoppablePost

interface HomeRepository {
    /** Discover tab — banners, new art, and popular curations in a single request. */
    suspend fun getDiscoverFeed(): ApiResult<HomeFeed>
    suspend fun getShopArtworks(page: Int, size: Int): ApiResult<List<ShoppablePost>>
    suspend fun likeArtwork(artworkId: Int): ApiResult<Unit>
    suspend fun unlikeArtwork(artworkId: Int): ApiResult<Unit>
}