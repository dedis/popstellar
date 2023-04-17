package com.github.dedis.popstellar.repository.remote;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.*;
import com.github.dedis.popstellar.model.network.method.*;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.*;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/** This class handles the JSON-RPC layer of the protocol */
public class LAONetworkManager implements MessageSender {

  private static final String TAG = LAONetworkManager.class.getSimpleName();

  private static final int MAX_REPROCESSING = 5;

  private final MessageHandler messageHandler;
  private final MultiConnection multiConnection;
  public final AtomicInteger requestCounter = new AtomicInteger();
  private final SchedulerProvider schedulerProvider;
  private final Gson gson;

  // A subject that represents unprocessed messages
  private final Subject<GenericMessage> unprocessed = PublishSubject.create();
  private final ConcurrentHashMap<GenericMessage, Integer> reprocessingCounter =
      new ConcurrentHashMap<>();
  private final Set<Channel> subscribedChannels;
  private final CompositeDisposable disposables = new CompositeDisposable();

  public LAONetworkManager(
      MessageHandler messageHandler,
      MultiConnection multiConnection,
      Gson gson,
      SchedulerProvider schedulerProvider,
      Set<Channel> subscribedChannels) {
    this.messageHandler = messageHandler;
    this.multiConnection = multiConnection;
    this.gson = gson;
    this.schedulerProvider = schedulerProvider;
    this.subscribedChannels = new HashSet<>(subscribedChannels);

    // Start the incoming message processing
    processIncomingMessages();
    // Start the routine aimed at resubscribing to channels when the connection is lost
    resubscribeToChannelOnReconnection();
  }

  private void resubscribeToChannelOnReconnection() {
    disposables.add(
        multiConnection
            .observeConnectionEvents() // Observe the events of a connection
            .subscribeOn(schedulerProvider.io())
            // Filter out events that are not related to a reconnection
            .filter(event -> event.getClass().equals(WebSocket.Event.OnConnectionOpened.class))
            // Subscribe to the stream and when a connection event is received, send a subscribe
            // message
            // for each channel we are supposed to be subscribed to.
            .subscribe(
                event ->
                    subscribedChannels.forEach(
                        channel ->
                            disposables.add(
                                subscribe(channel)
                                    .subscribe(
                                        () ->
                                            Log.d(TAG, "resubscription successful to :" + channel),
                                        error ->
                                            Log.d(TAG, "error on resubscription to" + error)))),
                error -> Log.d(TAG, "Error on resubscription : " + error)));
  }

  private void processIncomingMessages() {
    disposables.add(
        Observable.merge(
                // Normal message received over the wire
                multiConnection.observeMessage(),
                // Packets that could not be processed (maybe due to a reordering),
                // this is merged into incoming message,
                // with a delay of 5 seconds to give priority to new messages.
                unprocessed.delay(5, TimeUnit.SECONDS, schedulerProvider.computation()))
            .filter(Broadcast.class::isInstance) // Filter the Broadcast
            .map(Broadcast.class::cast)
            .subscribeOn(schedulerProvider.newThread())
            .subscribe(
                this::handleBroadcast,
                error -> Log.d(TAG, "Error on processing message: " + error)));
  }

  @Override
  public Completable catchup(Channel channel) {
    Log.d(TAG, "sending a catchup to the channel " + channel);
    Catchup catchup = new Catchup(channel, requestCounter.incrementAndGet());

    return request(catchup)
        .map(ResultMessages.class::cast)
        .map(ResultMessages::getMessages)
        .doOnError(error -> Log.d(TAG, "Error in catchup :" + error))
        .doOnSuccess(
            msgs -> Log.d(TAG, "Received catchup response on " + channel + ", retrieved : " + msgs))
        .doOnSuccess(messages -> handleMessages(messages, channel))
        .ignoreElement();
  }

  @Override
  public Completable publish(KeyPair keyPair, Channel channel, Data data) {
    return publish(channel, new MessageGeneral(keyPair, data, gson));
  }

  @Override
  public Completable publish(Channel channel, MessageGeneral msg) {
    Log.d(TAG, "sending a publish " + msg.getData().getClass() + " to the channel " + channel);
    Publish publish = new Publish(channel, requestCounter.incrementAndGet(), msg);
    return request(publish)
        .ignoreElement()
        .doOnComplete(() -> Log.d(TAG, "Successfully published " + msg));
  }

  @Override
  public Completable subscribe(Channel channel) {
    Log.d(TAG, "sending a subscribe on the channel " + channel);
    Subscribe subscribe = new Subscribe(channel, requestCounter.incrementAndGet());
    return request(subscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(
            answer -> {
              Log.d(TAG, "Adding " + channel + " to subscriptions");
              subscribedChannels.add(channel);
            })
        .doOnError(error -> Log.d(TAG, "error in subscribe : ", error))
        // Catchup already sent messages after the subscription to the channel is complete
        // This allows for the completion of the returned completable only when both subscribe
        // and catchup are completed
        .flatMapCompletable(answer -> catchup(channel))
        .doOnComplete(
            () -> Log.d(TAG, "Successfully subscribed and catchup to channel " + channel));
  }

  @Override
  public Completable unsubscribe(Channel channel) {
    Log.d(TAG, "sending an unsubscribe on the channel " + channel);
    Unsubscribe unsubscribe = new Unsubscribe(channel, requestCounter.incrementAndGet());
    return request(unsubscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(
            answer -> {
              Log.d(TAG, "Removing " + channel + " from subscriptions");
              subscribedChannels.remove(channel);
            })
        .doOnError(error -> Log.d(TAG, "error unsubscribing : ", error))
        .ignoreElement();
  }

  @Override
  public Observable<WebSocket.Event> getConnectEvents() {
    return multiConnection.observeConnectionEvents();
  }

  @Override
  public Set<Channel> getSubscriptions() {
    return new HashSet<>(subscribedChannels);
  }

  @Override
  public void extendConnection(List<PeerAddress> peerAddressList) {
    // If succeeded in extending the connections then return true
    if (multiConnection.connectToPeers(peerAddressList)) {
      // Start the incoming message processing for the other connections
      processIncomingMessages();
      // Start the routine aimed at resubscribing to channels when the connection is lost
      resubscribeToChannelOnReconnection();
    }
  }

  private void handleBroadcast(Broadcast broadcast) {
    Log.d(TAG, "handling broadcast msg : " + broadcast);
    try {
      messageHandler.handleMessage(this, broadcast.getChannel(), broadcast.getMessage());
    } catch (DataHandlingException
        | UnknownLaoException
        | UnknownRollCallException
        | NoRollCallException
        | UnknownElectionException e) {
      Log.e(TAG, "Error while handling received message", e);
      reprocessMessage(broadcast);
    }
  }

  private void handleMessages(List<MessageGeneral> messages, Channel channel) {
    for (MessageGeneral msg : messages) {
      try {
        messageHandler.handleMessage(this, channel, msg);
      } catch (DataHandlingException
          | UnknownLaoException
          | UnknownRollCallException
          | NoRollCallException
          | UnknownElectionException e) {
        Log.e(TAG, "Error while handling received catchup message", e);
      }
    }
  }

  private Single<Answer> request(Query query) {
    return multiConnection
        .observeMessage() // Observe incoming messages
        // Send the message upon subscription the the incoming messages. That way we are
        // certain the reply will be processed and the message is only sent when an observer
        // subscribes to the request answer.
        .doOnSubscribe(d -> multiConnection.sendMessage(query))
        .filter(Answer.class::isInstance) // Filter for Answers
        .map(Answer.class::cast)
        // This specific request has an id, only let the related Answer pass
        .filter(answer -> answer.getId() == query.getRequestId())
        .doOnNext(answer -> Log.d(TAG, "request id: " + answer.getId()))
        // Transform from an Observable to a Single
        // This Means that we expect a result before the source is disposed and an error
        // will be produced if no value is received.
        .firstOrError()
        // If we receive an error, transform the flow to a Failure
        .flatMap(
            answer -> {
              if (answer instanceof Error) {
                return Single.error(new JsonRPCErrorException((Error) answer));
              } else {
                return Single.just(answer);
              }
            })
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.mainThread())
        // Add a timeout to automatically dispose of the flow and end with a failure
        .timeout(5, TimeUnit.SECONDS)
        .cache();
  }

  /**
   * This function distinguish an unrecoverable failure based on the number of attempts of
   * reprocessing.
   *
   * @param message Message failed to be handled to be reprocessed
   */
  private void reprocessMessage(GenericMessage message) {
    // Check that the message hasn't already reprocessed more than the threshold of dropout
    int count = reprocessingCounter.getOrDefault(message, 0);
    if (count < MAX_REPROCESSING) {
      // Increase the counter and reprocess
      reprocessingCounter.put(message, count + 1);
      unprocessed.onNext(message);
    } else {
      // Discard the message
      reprocessingCounter.remove(message);
    }
  }

  @Override
  public void dispose() {
    disposables.dispose();
    multiConnection.close();
  }

  @Override
  public boolean isDisposed() {
    return disposables.isDisposed();
  }
}
