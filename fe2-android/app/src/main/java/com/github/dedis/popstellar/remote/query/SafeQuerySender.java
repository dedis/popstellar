package com.github.dedis.popstellar.remote.query;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.remote.Connection;
import com.tinder.scarlet.WebSocket;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

public class SafeQuerySender implements QuerySender {

  private final Set<String> subscribedChannels = new HashSet<>();

  private final QuerySender delegate;
  private final Disposable disposable;

  public SafeQuerySender(QuerySender delegate, Connection connection, Scheduler scheduler) {
    this.delegate = delegate;

    disposable =
        connection
            .observeWebsocket()
            .subscribeOn(scheduler)
            .filter(event -> event.getClass().equals(WebSocket.Event.OnConnectionOpened.class))
            .subscribe(event -> subscribedChannels.forEach(this::subscribe));
  }

  @Override
  public Completable catchup(String channel) {
    return delegate.catchup(channel);
  }

  @Override
  public Completable publish(KeyPair keyPair, String channel, Data data) {
    return delegate.publish(keyPair, channel, data);
  }

  @Override
  public Completable subscribe(String channel) {
    return delegate.subscribe(channel).doOnComplete(() -> subscribedChannels.add(channel));
  }

  @Override
  public Completable unsubscribe(String channel) {
    return delegate.unsubscribe(channel).doOnComplete(() -> subscribedChannels.remove(channel));
  }

  @Override
  public void dispose() {
    delegate.dispose();
    disposable.dispose();
  }

  @Override
  public boolean isDisposed() {
    return delegate.isDisposed() && disposable.isDisposed();
  }
}
