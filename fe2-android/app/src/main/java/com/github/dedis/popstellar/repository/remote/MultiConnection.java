package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.WebSocket;

import java.util.*;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class MultiConnection extends Connection {
  private ConnectionFactory connectionFactory;
  private final Map<PeerAddress, Connection> connectionMap = new HashMap<>();

  public MultiConnection(
      String url, LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    super(url, laoService, manualState);
  }

  public void setConnectionFactory(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public void connectToPeers(List<PeerAddress> peerAddressList) {
    peerAddressList.forEach(
        p -> connectionMap.put(p, connectionFactory.createConnection(p.getAddress())));
  }

  public io.reactivex.Observable<GenericMessage> observeMessage(boolean firstConnection) {
    if (firstConnection) {
      return super.observeMessage();
    }
    return connectionMap.values().stream()
        .map(Connection::observeMessage)
        .reduce(Observable::concatWith)
        .get();
  }

  public Observable<WebSocket.Event> observeConnectionEvents(boolean firstConnection) {
    if (firstConnection) {
      return super.observeConnectionEvents();
    }
    return connectionMap.values().stream()
        .map(Connection::observeConnectionEvents)
        .reduce(Observable::concatWith)
        .get();
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
