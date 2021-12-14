package com.github.dedis.popstellar.di;

import android.app.Application;

import com.github.dedis.popstellar.repository.remote.LAORequestFactory;
import com.github.dedis.popstellar.repository.remote.LAOService;
import com.google.gson.Gson;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter;
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

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

  private static final String DEFAULT_SERVER_URL = "ws://10.0.2.2:9000/organizer/client";

  private NetworkModule() {}

  @Provides
  @Singleton
  public static OkHttpClient provideOkHttpClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build();
  }

  @Provides
  @Singleton
  public static LAORequestFactory provideRequestFactory() {
    return new LAORequestFactory(DEFAULT_SERVER_URL);
  }

  @Provides
  @Singleton
  public static Scarlet provideScarlet(
      Application application,
      OkHttpClient okHttpClient,
      Gson gson,
      LAORequestFactory requestFactory) {
    return new Scarlet.Builder()
        .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okHttpClient, requestFactory))
        .addMessageAdapterFactory(new GsonMessageAdapter.Factory(gson))
        .addStreamAdapterFactory(new RxJava2StreamAdapterFactory())
        .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
        // .backoffStrategy(new ExponentialBackoffStrategy())
        .build();
  }

  @Provides
  @Singleton
  public static LAOService provideLAOService(Scarlet scarlet) {
    return scarlet.create(LAOService.class);
  }
}
