package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.tinder.scarlet.WebSocket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  private final ConcurrentHashMap<PeerAddress, Connection> connectionMap;

  /**
   * Create the main connection as the super class
   *
   * @param connectionProvider functional interface to call the createConnection of the
   *     connectionFactory
   * @param url main server's url
   */
  public MultiConnection(Function<String, Connection> connectionProvider, String url) {
    // Instantiate the first connection to the main server
    super(connectionProvider.apply(url));
    this.connectionProvider = connectionProvider;
    connectionMap = new ConcurrentHashMap<>();
  }

  /**
   * Function called upon the GreetLao, it extends the connection for all the new peers.
   *
   * @param peerAddressList list of peer servers contained in the greet message
   * @return true if the connection is extended, false if all the peers are already connected
   */
  public boolean connectToPeers(List<PeerAddress> peerAddressList) {
    // Extract the peers for which there's no connection already
    List<PeerAddress> newPeers =
        peerAddressList.stream()
            .filter(peer -> !connectionMap.containsKey(peer))
            .collect(Collectors.toList());
    if (newPeers.isEmpty()) {
      return false;
    }
    newPeers.forEach(p -> connectionMap.put(p, connectionProvider.apply(p.getAddress())));
    return true;
  }

  /**
   * Function to observe messages on the connections.
   *
   * @return an Observable of GenericMessage received on the connections
   */
  @Override
  public io.reactivex.Observable<GenericMessage> observeMessage() {
    return super.observeMessage()
        .concatWith(
            connectionMap.values().stream()
                .map(Connection::observeMessage)
                .reduce(Observable::concatWith)
                .orElse(Observable.empty()));
  }

  /**
   * Function to observe events on the connections.
   *
   * @return an Observable of Events happening on the connection
   */
  @Override
  public Observable<WebSocket.Event> observeConnectionEvents() {
    return super.observeConnectionEvents()
        .concatWith(
            connectionMap.values().stream()
                .map(Connection::observeConnectionEvents)
                .reduce(Observable::concatWith)
                .orElse(Observable.empty()));
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
