package com.github.dedis.popstellar.repository.remote;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.*;
import com.github.dedis.popstellar.model.network.method.*;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/** This class handles the JSON-RPC layer of the protocol */
public class LAONetworkManager implements MessageSender {

  private static final String TAG = LAONetworkManager.class.getSimpleName();

  private final MessageRepository messageRepository;
  private final LAORepository laoRepository;
  private final MessageHandler messageHandler;
  private final Connection connection;
  public final AtomicInteger requestCounter = new AtomicInteger();
  private final SchedulerProvider schedulerProvider;
  private final Gson gson;

  // A subject that represents unprocessed messages
  private final Subject<GenericMessage> unprocessed = PublishSubject.create();
  private final List<Channel> subscribedChannels = new LinkedList<>();
  private final CompositeDisposable disposables = new CompositeDisposable();

  public LAONetworkManager(
      MessageRepository messageRepository,
      LAORepository laoRepository,
      MessageHandler messageHandler,
      Connection connection,
      Gson gson,
      SchedulerProvider schedulerProvider) {
    this.messageRepository = messageRepository;
    this.laoRepository = laoRepository;
    this.messageHandler = messageHandler;
    this.connection = connection;
    this.gson = gson;
    this.schedulerProvider = schedulerProvider;

    // Start the incoming message processing
    processIncomingMessages();
    // Start the routine aimed at resubscribing to channels when the connection is lost
    resubscribeToChannelOnReconnection();
  }

  private void resubscribeToChannelOnReconnection() {
    disposables.add(
        connection
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
                connection.observeMessage(),
                // Packets that could not be processed (maybe due to a reordering), this is merged
                // into
                // incoming message with a delay of 5 seconds to give priority to new messages.
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
        .doOnError(error -> Log.d(TAG, "error in subscribe : " + error))
        // This is used when reconnecting after a lost connection
        .doOnSuccess(answer -> subscribedChannels.add(channel))
        .doOnSuccess(a -> Log.d(TAG, "Subscribed to " + channel + ", sending catchup message"))
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
        .doOnSuccess(answer -> subscribedChannels.remove(channel))
        .doOnSuccess(a -> Log.d(TAG, "Unsubscribed from " + channel + ""))
        .doOnError(error -> Log.d(TAG, "error unsubscribing : " + error))
        .ignoreElement();
  }

  @Override
  public Observable<WebSocket.Event> getConnectEvents() {
    return connection.observeConnectionEvents();
  }

  private void handleBroadcast(Broadcast broadcast) {
    Log.d(TAG, "handling broadcast msg : " + broadcast);
    try {
      messageHandler.handleMessage(
          messageRepository, laoRepository, this, broadcast.getChannel(), broadcast.getMessage());
    } catch (DataHandlingException | UnknownLaoException e) {
      Log.e(TAG, "Error while handling received message", e);
      unprocessed.onNext(broadcast);
    }
  }

  private void handleMessages(List<MessageGeneral> messages, Channel channel) {
    for (MessageGeneral msg : messages) {
      try {
        messageHandler.handleMessage(messageRepository, laoRepository, this, channel, msg);
      } catch (DataHandlingException | UnknownLaoException e) {
        Log.e(TAG, "Error while handling received catchup message", e);
      }
    }
  }

  private Single<Answer> request(Query query) {
    return connection
        .observeMessage() // Observe incoming messages
        // Send the message upon subscription the the incoming messages. That way we are
        // certain the reply will be processed and the message is only sent when an observer
        // subscribes to the request answer.
        .doOnSubscribe(d -> connection.sendMessage(query))
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

  @Override
  public void dispose() {
    disposables.dispose();
    connection.close();
  }

  @Override
  public boolean isDisposed() {
    return disposables.isDisposed();
  }
}
