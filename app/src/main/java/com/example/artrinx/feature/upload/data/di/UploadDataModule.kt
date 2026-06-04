package com.example.artrinx.feature.upload.data.di

import com.example.artrinx.feature.upload.data.remote.CurationApiService
import com.example.artrinx.feature.upload.data.remote.UploadApiService
import com.example.artrinx.feature.upload.data.repository.CurationRepositoryImpl
import com.example.artrinx.feature.upload.data.repository.UploadRepositoryImpl
import com.example.artrinx.feature.upload.domain.repository.CurationRepository
import com.example.artrinx.feature.upload.domain.repository.UploadRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadDataModule {

    @Binds
    @Singleton
    abstract fun bindUploadRepository(impl: UploadRepositoryImpl): UploadRepository

    @Binds
    @Singleton
    abstract fun bindCurationRepository(impl: CurationRepositoryImpl): CurationRepository

    companion object {
        // Steps 1 & 3 use the MAIN authed Retrofit. Step 2 (signed-URL PUT) uses the bare
        // @Named("upload") OkHttpClient directly inside the repository.
        @Provides
        @Singleton
        fun provideUploadApiService(retrofit: Retrofit): UploadApiService =
            retrofit.create(UploadApiService::class.java)

        @Provides
        @Singleton
        fun provideCurationApiService(retrofit: Retrofit): CurationApiService =
            retrofit.create(CurationApiService::class.java)
    }
}
