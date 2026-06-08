package com.rinx.artRINXapp.feature.auth.data.di

import com.rinx.artRINXapp.feature.auth.data.remote.AuthApiService
import com.rinx.artRINXapp.feature.auth.data.repository.AuthRepositoryImpl
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {

        @Provides
        @Singleton
        fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
            retrofit.create(AuthApiService::class.java)
    }
}
