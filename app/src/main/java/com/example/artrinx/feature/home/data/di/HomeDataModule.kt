package com.example.artrinx.feature.home.data.di

import com.example.artrinx.feature.home.data.remote.HomeApiService
import com.example.artrinx.feature.home.data.repository.HomeRepositoryImpl
import com.example.artrinx.feature.home.domain.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    companion object {
        @Provides
        @Singleton
        fun provideHomeApiService(retrofit: Retrofit): HomeApiService =
            retrofit.create(HomeApiService::class.java)
    }
}