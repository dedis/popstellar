package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.tinder.scarlet.WebSocket;

import java.util.*;
import java.util.function.Function;

import io.reactivex.Observable;

/**
 * This class extends Connection to represent a set of Connections to multiple servers.
 *
 * <p>The superclass works as main connection to the first server, the others are instantiated in
 * the connectionMap.
 */
public class MultiConnection extends Connection {

  /** Caller method for the connectionFactory to instantiate connections */
  private final Function<String, Connection> connectionProvider;

  /** Map a PeerAddress (url for now) to its connection */
  private final Map<PeerAddress, Connection> connectionMap = new HashMap<>();

  /**
   * Create the main connection as the super class
   *
   * @param connectionProvider functional interface to call the createConnection of the
   *     connectionFactory
   * @param url main server's url
   */
  public MultiConnection(Function<String, Connection> connectionProvider, String url) {
    super(connectionProvider.apply(url));
    this.connectionProvider = connectionProvider;
  }

  /**
   * Function called upon the GreetLao, it extends the connection only for the first GreetLao
   * received.
   *
   * @param peerAddressList list of peer servers contained in the greet message
   * @return true if the connection hasn't been extended already, false otherwise
   */
  public boolean connectToPeers(List<PeerAddress> peerAddressList) {
    // If the connection to peers has already established don't connect to
    // the peers of the peers (depth = 1)
    if (!connectionMap.isEmpty()) {
      return false;
    }
    peerAddressList.forEach(p -> connectionMap.put(p, connectionProvider.apply(p.getAddress())));
    return true;
  }

  /**
   * Function to observe messages on the connections.
   *
   * @param firstConnection boolean to indicate whether we want to observe messages from the first
   *     main connection or from the others.
   * @return an Observable of GenericMessage received on the connections
   */
  public io.reactivex.Observable<GenericMessage> observeMessage(boolean firstConnection) {
    // As the first connection has to be made at the start, we need first to observe on that
    // to deliver the GreetLao and then extend to multiple connections
    if (firstConnection) {
      return super.observeMessage();
    }
    return connectionMap.values().stream()
        .map(Connection::observeMessage)
        .reduce(Observable::concatWith)
        .orElse(Observable.empty());
  }

  /**
   * Function to observe events on the connections.
   *
   * @param firstConnection boolean to indicate whether we want to observe messages from the first
   *     main connection or from the others.
   * @return an Observable of Events happening on the connection
   */
  public Observable<WebSocket.Event> observeConnectionEvents(boolean firstConnection) {
    // As the first connection has to be made at the start, we need first to observe on that
    // to deliver the GreetLao and then extend to multiple connections
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
