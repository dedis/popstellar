package com.github.dedis.popstellar.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @JvmStatic
  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    return Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()
  }
}
