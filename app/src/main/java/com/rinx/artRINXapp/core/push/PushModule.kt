package com.rinx.artRINXapp.core.push

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PushModule {

    @Provides
    @Singleton
    fun providePushApiService(retrofit: Retrofit): PushApiService =
        retrofit.create(PushApiService::class.java)
}
