package com.github.dedis.popstellar.repository.remote;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;

/** Module responsible of managing the connection to the backend server. */
@Singleton
public class GlobalNetworkManager implements Disposable {

  private static final String DEFAULT_URL = "ws://10.0.2.2:9000/client";

  private final MessageHandler messageHandler;
  private final ConnectionFactory connectionFactory;
  private final Gson gson;
  private final SchedulerProvider schedulerProvider;

  @Nullable private MessageSender networkManager;
  @Nullable private String currentURL;

  @Inject
  public GlobalNetworkManager(
      MessageHandler messageHandler,
      ConnectionFactory connectionFactory,
      Gson gson,
      SchedulerProvider schedulerProvider) {
    this.messageHandler = messageHandler;
    this.connectionFactory = connectionFactory;
    this.gson = gson;
    this.schedulerProvider = schedulerProvider;

    connect(DEFAULT_URL);
  }

  public void connect(String url) {
    connect(url, new HashSet<>());
  }

  public void connect(String url, Set<Channel> subscriptions) {
    if (networkManager != null) {
      networkManager.dispose();
    }

    Connection connection = connectionFactory.createConnection(url);
    networkManager =
        new LAONetworkManager(messageHandler, connection, gson, schedulerProvider, subscriptions);
    currentURL = url;
  }

  public String getCurrentUrl() {
    return currentURL;
  }

  @NonNull
  public MessageSender getMessageSender() {
    if (networkManager == null) {
      throw new IllegalStateException("The connection has not been established.");
    }
    return networkManager;
  }

  @Override
  public void dispose() {
    if (networkManager != null) {
      networkManager.dispose();
    }
    networkManager = null;
  }

  @Override
  public boolean isDisposed() {
    return networkManager == null;
  }
}
