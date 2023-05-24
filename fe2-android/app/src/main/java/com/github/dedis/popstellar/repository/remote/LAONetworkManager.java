package com.github.dedis.popstellar.repository.remote;

import androidx.annotation.VisibleForTesting;

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
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

/** This class handles the JSON-RPC layer of the protocol */
public class LAONetworkManager implements MessageSender {

  private static final String TAG = LAONetworkManager.class.getSimpleName();

  /** Constants to tune the reprocessing of unhandled messages */
  public static final int MAX_REPROCESSING = 5;

  public static final int REPROCESSING_DELAY = 5;

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
                                            Timber.tag(TAG)
                                                .d("resubscription successful to : %s", channel),
                                        error ->
                                            Timber.tag(TAG)
                                                .d(error, "error on resubscription to")))),
                error -> Timber.tag(TAG).d(error, "Error on resubscription")));
  }

  private void processIncomingMessages() {
    disposables.add(
        Observable.merge(
                // Normal message received over the wire
                multiConnection.observeMessage(),
                // Packets that could not be processed (maybe due to a reordering),
                // this is merged into incoming message,
                // with a delay of 5 seconds to give priority to new messages.
                unprocessed.delay(
                    REPROCESSING_DELAY, TimeUnit.SECONDS, schedulerProvider.computation()))
            .filter(Broadcast.class::isInstance) // Filter the Broadcast
            .map(Broadcast.class::cast)
            .subscribeOn(schedulerProvider.newThread())
            .subscribe(
                this::handleBroadcast,
                error -> Timber.tag(TAG).d(error, "Error on processing message")));
  }

  @Override
  public Completable catchup(Channel channel) {
    Timber.tag(TAG).d("sending a catchup to the channel %s", channel);
    Catchup catchup = new Catchup(channel, requestCounter.incrementAndGet());

    return request(catchup)
        .map(ResultMessages.class::cast)
        .map(ResultMessages::getMessages)
        .doOnError(error -> Timber.tag(TAG).d(error, "Error in catchup"))
        .doOnSuccess(
            msgs ->
                Timber.tag(TAG).d("Received catchup response on %s, retrieved : %s", channel, msgs))
        .doOnSuccess(messages -> handleMessages(messages, channel))
        .ignoreElement();
  }

  @Override
  public Completable publish(KeyPair keyPair, Channel channel, Data data) {
    return publish(channel, new MessageGeneral(keyPair, data, gson));
  }

  @Override
  public Completable publish(Channel channel, MessageGeneral msg) {
    Timber.tag(TAG).d("sending a publish %s to the channel %s", msg.getData().getClass(), channel);
    Publish publish = new Publish(channel, requestCounter.incrementAndGet(), msg);
    return request(publish)
        .ignoreElement()
        .doOnComplete(() -> Timber.tag(TAG).d("Successfully published %s", msg));
  }

  @Override
  public Completable subscribe(Channel channel) {
    Timber.tag(TAG).d("sending a subscribe on the channel %s", channel);
    Subscribe subscribe = new Subscribe(channel, requestCounter.incrementAndGet());
    return request(subscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(
            answer -> {
              Timber.tag(TAG).d("Adding %s to subscriptions", channel);
              subscribedChannels.add(channel);
            })
        .doOnError(error -> Timber.tag(TAG).d(error, "error in subscribe"))
        // Catchup already sent messages after the subscription to the channel is complete
        // This allows for the completion of the returned completable only when both subscribe
        // and catchup are completed
        .flatMapCompletable(answer -> catchup(channel))
        .doOnComplete(
            () -> Timber.tag(TAG).d("Successfully subscribed and catchup to channel %s", channel));
  }

  @Override
  public Completable unsubscribe(Channel channel) {
    Timber.tag(TAG).d("sending an unsubscribe on the channel %s", channel);
    Unsubscribe unsubscribe = new Unsubscribe(channel, requestCounter.incrementAndGet());
    return request(unsubscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(
            answer -> {
              Timber.tag(TAG).d("Removing %s from subscriptions", channel);
              subscribedChannels.remove(channel);
            })
        .doOnError(error -> Timber.tag(TAG).d(error, "error unsubscribing"))
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
    Timber.tag(TAG).d("handling broadcast msg : %s", broadcast);
    try {
      messageHandler.handleMessage(this, broadcast.getChannel(), broadcast.getMessage());
    } catch (DataHandlingException
        | UnknownLaoException
        | UnknownRollCallException
        | NoRollCallException
        | UnknownElectionException
        | UnknownWitnessMessageException e) {
      Timber.tag(TAG).e(e, "Error while handling received message, will try to reprocess it later");
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
          | UnknownElectionException
          | UnknownWitnessMessageException e) {
        Timber.tag(TAG).e(e, "Error while handling received catchup message");
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
        .doOnNext(answer -> Timber.tag(TAG).d("request id: %s", answer.getId()))
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
        .timeout(REPROCESSING_DELAY, TimeUnit.SECONDS)
        .cache();
  }

  /**
   * This function distinguishes an unrecoverable failure according to the number of reprocessing
   * attempts.
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
      Timber.tag(TAG)
          .d("Message %s has been reprocessed too many times, it's now dropped", message);
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

  @VisibleForTesting
  public TestObserver<GenericMessage> testUnprocessed() {
    return unprocessed.test();
  }
}
