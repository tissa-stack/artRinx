package com.example.artrinx.feature.notifications.data.di

import com.example.artrinx.feature.notifications.data.remote.NotificationsApiService
import com.example.artrinx.feature.notifications.data.repository.NotificationsRepositoryImpl
import com.example.artrinx.feature.notifications.domain.repository.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsDataModule {

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository

    companion object {
        @Provides
        @Singleton
        fun provideNotificationsApiService(retrofit: Retrofit): NotificationsApiService =
            retrofit.create(NotificationsApiService::class.java)
    }
}
