package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.method.Message
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.tinder.scarlet.WebSocket
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

/**
 * This class extends Connection to represent a set of Connections to multiple servers.
 *
 * The superclass works as main connection to the first server, the others are instantiated in the
 * connectionMap.
 */
class MultiConnection(
    /** Caller method for the connectionFactory to instantiate connections */
    private val connectionProvider: Function<String, Connection>,
    url: String
) :
    Connection(
        // Instantiate the first connection to the main server
        connectionProvider.apply(url)) {
  /** Map a PeerAddress (url for now) to its connection */
  private val connectionMap: ConcurrentHashMap<PeerAddress, Connection> = ConcurrentHashMap()

  /**
   * Function called upon the GreetLao, it extends the connection for all the new peers.
   *
   * @param peerAddressList list of peer servers contained in the greet message
   * @return true if the connection is extended, false if all the peers are already connected
   */
  fun connectToPeers(peerAddressList: List<PeerAddress>): Boolean {
    // Extract the peers for which there's no connection already
    val newPeers =
        peerAddressList
            .stream()
            .filter { peer: PeerAddress -> !connectionMap.containsKey(peer) }
            .collect(Collectors.toList())
    if (newPeers.isEmpty()) {
      return false
    }
    newPeers.forEach(
        Consumer { p: PeerAddress -> connectionMap[p] = connectionProvider.apply(p.address) })
    return true
  }

  /**
   * Function to observe messages on the connections.
   *
   * @return an Observable of GenericMessage received on the connections
   */
  override fun observeMessage(): Observable<GenericMessage?> {
    return super.observeMessage()
        .concatWith(
            connectionMap.values
                .stream()
                .map { obj: Connection -> obj.observeMessage() }
                .reduce { obj: Observable<GenericMessage?>, other: Observable<GenericMessage?> ->
                  obj.concatWith(other)
                }
                .orElse(Observable.empty<GenericMessage?>()))
  }

  /**
   * Function to observe events on the connections.
   *
   * @return an Observable of Events happening on the connection
   */
  override fun observeConnectionEvents(): Observable<WebSocket.Event?> {
    return super.observeConnectionEvents()
        .concatWith(
            connectionMap.values
                .stream()
                .map { obj: Connection -> obj.observeConnectionEvents() }
                .reduce { obj: Observable<WebSocket.Event?>, other: Observable<WebSocket.Event?> ->
                  obj.concatWith(other)
                }
                .orElse(Observable.empty<WebSocket.Event?>()))
  }

  override fun sendMessage(msg: Message) {
    super.sendMessage(msg)
    connectionMap.values.forEach(Consumer { peer: Connection -> peer.sendMessage(msg) })
  }

  override fun close() {
    super.close()
    connectionMap.values.forEach(Consumer { obj: Connection -> obj.close() })
  }
}
