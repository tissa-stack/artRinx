package com.rinx.artRINXapp.feature.events.data.di

import com.rinx.artRINXapp.feature.events.data.remote.EventsApiService
import com.rinx.artRINXapp.feature.events.data.repository.EventsRepositoryImpl
import com.rinx.artRINXapp.feature.events.domain.repository.EventsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsDataModule {

    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    companion object {
        @Provides
        @Singleton
        fun provideEventsApiService(retrofit: Retrofit): EventsApiService =
            retrofit.create(EventsApiService::class.java)
    }
}
