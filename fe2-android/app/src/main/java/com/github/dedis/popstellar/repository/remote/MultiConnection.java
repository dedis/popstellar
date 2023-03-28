package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.tinder.scarlet.WebSocket;

import java.util.*;

import javax.inject.Inject;

import io.reactivex.Observable;

public class MultiConnection implements LAOService {
  private final Map<PeerAddress, Connection> peerConnections = new HashMap<>();
  private final Connection firstConnection;
  @Inject ConnectionFactory connectionFactory;

  public MultiConnection(Connection firstConnection) {
    this.firstConnection = firstConnection;
  }

  public void extendConnections(List<PeerAddress> peerAddressList) {
    for (PeerAddress p : peerAddressList) {
      peerConnections.put(p, connectionFactory.createConnection(p.getAddress()));
    }
  }

  @Override
  public void sendMessage(Message msg) {
    firstConnection.sendMessage(msg);
    for (Connection c : peerConnections.values()) {
      c.sendMessage(msg);
    }
  }

  @Override
  public Observable<GenericMessage> observeMessage() {
    return firstConnection.observeMessage();
  }

  @Override
  public Observable<WebSocket.Event> observeWebsocket() {
    return firstConnection.observeWebsocket();
  }

  public void close() {
    firstConnection.close();
    peerConnections.values().forEach(Connection::close);
  }
}
