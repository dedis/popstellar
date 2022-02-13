package com.github.dedis.popstellar.remote;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.remote.query.QueryManager;
import com.github.dedis.popstellar.remote.query.QuerySender;
import com.github.dedis.popstellar.remote.query.SafeQuerySender;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;

@Singleton
public class ConnectionManager implements Disposable {

  private static final String DEFAULT_URL = "ws://10.0.2.2:9000/organizer/client";

  private final LAORepository laoRepository;
  private final MessageHandler messageHandler;
  private final ConnectionFactory connectionFactory;
  private final Gson gson;
  private final SchedulerProvider schedulerProvider;

  @Nullable private QuerySender currentQuerySender;

  @Inject
  public ConnectionManager(
      LAORepository laoRepository,
      MessageHandler messageHandler,
      ConnectionFactory connectionFactory,
      Gson gson,
      SchedulerProvider schedulerProvider) {
    this.laoRepository = laoRepository;
    this.messageHandler = messageHandler;
    this.connectionFactory = connectionFactory;
    this.gson = gson;
    this.schedulerProvider = schedulerProvider;

    connect(DEFAULT_URL);
  }

  public void connect(String url) {
    if (currentQuerySender != null) currentQuerySender.dispose();

    Connection connection = connectionFactory.createConnection(url);
    QuerySender querySender =
        new QueryManager(laoRepository, messageHandler, connection, gson, schedulerProvider);
    currentQuerySender = new SafeQuerySender(querySender, connection, schedulerProvider.io());
  }

  @NonNull
  public QuerySender getQuerySender() {
    if (currentQuerySender == null)
      throw new IllegalStateException("The connection has not been established.");
    return currentQuerySender;
  }

  @Override
  public void dispose() {
    if (currentQuerySender != null) currentQuerySender.dispose();
    currentQuerySender = null;
  }

  @Override
  public boolean isDisposed() {
    return currentQuerySender == null;
  }
}
