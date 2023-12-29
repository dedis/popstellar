package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.tinder.scarlet.WebSocket
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Interface exposing the JSON-RPC layer of the protocol.
 *
 * It can be used to send requests to the linked backend and wait for answers. [ ] are used as
 * callbacks to give the result of the request.
 */
interface MessageSender : Disposable {
  /**
   * Catchup on the given channel
   *
   * @param channel to retrieve old messages on
   * @return a [Completable] the will complete once the catchup is finished
   */
  fun catchup(channel: Channel): Completable

  /**
   * Publish some [Data] on the given channel
   *
   * @param keyPair used to sign the sent message
   * @param channel to send the data on
   * @param data to send
   * @return a [Completable] the will complete once the publish is finished
   */
  fun publish(keyPair: KeyPair, channel: Channel, data: Data): Completable

  /**
   * Publish an already created [MessageGeneral] on the given channel
   *
   * @param channel to send the data on
   * @param msg to send
   * @return a [Completable] the will complete once the publish is finished
   */
  fun publish(channel: Channel, msg: MessageGeneral): Completable

  /**
   * Subscribe to given channel
   *
   * When the subscription is complete, the system will automatically catchup on that channel
   *
   * @param channel to subscribe to
   * @return a [Completable] the will complete once the subscription is finished
   */
  fun subscribe(channel: Channel): Completable

  /**
   * Unsubscribe of given channel
   *
   * @param channel to unsubscribe from
   * @return a [Completable] the will complete once the unsubscribe is finished
   */
  fun unsubscribe(channel: Channel): Completable

  /** @return an Observable of WebSocket events of the underlying connection */
  val connectEvents: Observable<WebSocket.Event?>

  /** @return the list of channels we subscribed to */
  val subscriptions: Set<Channel?>

  /**
   * Extend the connection by connecting to the peers of a server upon a GreetLao
   *
   * @param peerAddressList list of peers to be connected
   */
  fun extendConnection(peerAddressList: List<PeerAddress>)
}
