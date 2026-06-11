package com.rinx.artRINXapp.feature.share.data.di

import com.rinx.artRINXapp.feature.share.data.remote.ShareApiService
import com.rinx.artRINXapp.feature.share.data.repository.ShareRepositoryImpl
import com.rinx.artRINXapp.feature.share.domain.repository.ShareRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareDataModule {

    @Binds
    @Singleton
    abstract fun bindShareRepository(impl: ShareRepositoryImpl): ShareRepository

    companion object {
        @Provides
        @Singleton
        fun provideShareApiService(retrofit: Retrofit): ShareApiService =
            retrofit.create(ShareApiService::class.java)
    }
}
