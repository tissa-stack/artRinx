package com.rinx.artRINXapp.feature.profile.data.di

import com.rinx.artRINXapp.feature.profile.data.remote.LocationApiService
import com.rinx.artRINXapp.feature.profile.data.remote.ProfileApiService
import com.rinx.artRINXapp.feature.profile.data.repository.MasterLocationRepositoryImpl
import com.rinx.artRINXapp.feature.profile.data.repository.ProfileRepositoryImpl
import com.rinx.artRINXapp.feature.profile.domain.repository.MasterLocationRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
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

    @Binds
    @Singleton
    abstract fun bindMasterLocationRepository(impl: MasterLocationRepositoryImpl): MasterLocationRepository

    companion object {
        @Provides
        @Singleton
        fun provideProfileApiService(retrofit: Retrofit): ProfileApiService =
            retrofit.create(ProfileApiService::class.java)

        @Provides
        @Singleton
        fun provideLocationApiService(retrofit: Retrofit): LocationApiService =
            retrofit.create(LocationApiService::class.java)
    }
}
