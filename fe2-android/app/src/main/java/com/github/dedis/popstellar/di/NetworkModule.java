package com.github.dedis.popstellar.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

  private NetworkModule() {}

  @Provides
  @Singleton
  public static OkHttpClient provideOkHttpClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build();
  }
}
