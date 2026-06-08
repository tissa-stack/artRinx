package com.rinx.artRINXapp.feature.notifications.data.di

import com.rinx.artRINXapp.feature.notifications.data.remote.MessagesApiService
import com.rinx.artRINXapp.feature.notifications.data.repository.MessagesRepositoryImpl
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagesDataModule {

    @Binds
    @Singleton
    abstract fun bindMessagesRepository(impl: MessagesRepositoryImpl): MessagesRepository

    companion object {
        @Provides
        @Singleton
        fun provideMessagesApiService(retrofit: Retrofit): MessagesApiService =
            retrofit.create(MessagesApiService::class.java)
    }
}
