package com.example.artrinx.feature.search.data.di

import com.example.artrinx.feature.search.data.remote.SearchApiService
import com.example.artrinx.feature.search.data.repository.SearchRepositoryImpl
import com.example.artrinx.feature.search.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchDataModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    companion object {
        @Provides
        @Singleton
        fun provideSearchApiService(retrofit: Retrofit): SearchApiService =
            retrofit.create(SearchApiService::class.java)
    }
}