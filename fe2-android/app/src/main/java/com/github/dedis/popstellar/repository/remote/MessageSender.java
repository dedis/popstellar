package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.tinder.scarlet.WebSocket;

import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Interface exposing the JSON-RPC layer of the protocol.
 *
 * <p>It can be used to send requests to the linked backend and wait for answers. {@link
 * Completable} are used as callbacks to give the result of the request.
 */
public interface MessageSender extends Disposable {

  /**
   * Catchup on the given channel
   *
   * @param channel to retrieve old messages on
   * @return a {@link Completable} the will complete once the catchup is finished
   */
  Completable catchup(Channel channel);

  /**
   * Publish some {@link Data} on the given channel
   *
   * @param keyPair used to sign the sent message
   * @param channel to send the data on
   * @param data to send
   * @return a {@link Completable} the will complete once the publish is finished
   */
  Completable publish(KeyPair keyPair, Channel channel, Data data);

  /**
   * Publish an already created {@link MessageGeneral} on the given channel
   *
   * @param channel to send the data on
   * @param msg to send
   * @return a {@link Completable} the will complete once the publish is finished
   */
  Completable publish(Channel channel, MessageGeneral msg);

  /**
   * Subscribe to given channel
   *
   * <p>When the subscription is complete, the system will automatically catchup on that channel
   *
   * @param channel to subscribe to
   * @return a {@link Completable} the will complete once the subscription is finished
   */
  Completable subscribe(Channel channel);

  /**
   * Unsubscribe of given channel
   *
   * @param channel to unsubscribe from
   * @return a {@link Completable} the will complete once the unsubscribe is finished
   */
  Completable unsubscribe(Channel channel);

  /**
   * @return an Observable of WebSocket events of the underlying connection
   */
  Observable<WebSocket.Event> getConnectEvents();

  /**
   * @return the set of channel subscribed to
   */
  Set<Channel> getSubscriptions();
}
