package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.tinder.scarlet.WebSocket;

import java.util.*;
import java.util.function.Function;

import io.reactivex.Observable;

public class MultiConnection extends Connection {
  private Function<String, Connection> connectionProvider;
  private final Map<PeerAddress, Connection> connectionMap = new HashMap<>();

  public MultiConnection(Connection connection) {
    super(connection);
  }

  public void setConnectionProvider(Function<String, Connection> connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public void connectToPeers(List<PeerAddress> peerAddressList) {
    peerAddressList.forEach(p -> connectionMap.put(p, connectionProvider.apply(p.getAddress())));
  }

  public io.reactivex.Observable<GenericMessage> observeMessage(boolean firstConnection) {
    if (firstConnection) {
      return super.observeMessage();
    }
    return connectionMap.values().stream()
        .map(Connection::observeMessage)
        .reduce(Observable::concatWith)
        .orElse(Observable.empty());
  }

  public Observable<WebSocket.Event> observeConnectionEvents(boolean firstConnection) {
    if (firstConnection) {
      return super.observeConnectionEvents();
    }
    return connectionMap.values().stream()
        .map(Connection::observeConnectionEvents)
        .reduce(Observable::concatWith)
        .orElse(Observable.empty());
  }

  @Override
  public void sendMessage(Message msg) {
    super.sendMessage(msg);
    connectionMap.values().forEach(peer -> peer.sendMessage(msg));
  }

  @Override
  public void close() {
    super.close();
    connectionMap.values().forEach(Connection::close);
  }
}
