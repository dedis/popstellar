package com.github.dedis.popstellar.repository.remote;

import android.app.Application;

import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.lifecycle.FlowableLifecycle;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter;
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;

/** Factory used to produce {@link Connection} to a specified URL */
public class ConnectionFactory {

  private final Application application;
  private final SchedulerProvider schedulerProvider;
  private final OkHttpClient okHttpClient;
  private final Gson gson;

  @Inject
  public ConnectionFactory(
      Application application,
      SchedulerProvider schedulerProvider,
      OkHttpClient okHttpClient,
      Gson gson) {
    this.application = application;
    this.schedulerProvider = schedulerProvider;
    this.okHttpClient = okHttpClient;
    this.gson = gson;
  }

  private Connection createConnection(String url) {
    // Create a behavior subject that will be used to close or start the socket manually
    BehaviorSubject<Lifecycle.State> manualState =
        BehaviorSubject.createDefault(Lifecycle.State.Started.INSTANCE);

    // Create the Scarlet instance
    Scarlet scarlet =
        new Scarlet.Builder()
            .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okHttpClient, url))
            .addMessageAdapterFactory(new GsonMessageAdapter.Factory(gson))
            .addStreamAdapterFactory(new RxJava2StreamAdapterFactory())
            .lifecycle(
                AndroidLifecycle.ofApplicationForeground(application)
                    .combineWith(
                        new FlowableLifecycle(
                            manualState.toFlowable(BackpressureStrategy.LATEST),
                            schedulerProvider.computation())))
            // .backoffStrategy(new ExponentialBackoffStrategy())
            .build();

    // And return a bundled object of the service and the subject
    return new Connection(url, scarlet.create(LAOService.class), manualState);
  }

  public MultiConnection createMultiConnection(String url) {
    return new MultiConnection(this::createConnection, url);
  }
}
