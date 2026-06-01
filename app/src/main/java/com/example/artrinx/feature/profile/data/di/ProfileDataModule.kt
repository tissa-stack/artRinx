package com.example.artrinx.feature.profile.data.di

import com.example.artrinx.feature.profile.data.remote.ProfileApiService
import com.example.artrinx.feature.profile.data.repository.ProfileRepositoryImpl
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileDataModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideProfileApiService(retrofit: Retrofit): ProfileApiService =
            retrofit.create(ProfileApiService::class.java)
    }
}
