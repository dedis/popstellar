package com.github.dedis.popstellar.repository.remote;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.Catchup;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.Query;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.github.dedis.popstellar.model.network.method.Unsubscribe;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.JsonRPCErrorException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/** This class handles the JSON-RPC layer of the protocol */
public class LAONetworkManager implements MessageSender {

  private static final String TAG = LAONetworkManager.class.getSimpleName();

  private final LAORepository repository;
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
      LAORepository repository,
      MessageHandler messageHandler,
      Connection connection,
      Gson gson,
      SchedulerProvider schedulerProvider) {
    this.repository = repository;
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
                        channel -> disposables.add(subscribe(channel).subscribe()))));
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
            .subscribeOn(schedulerProvider.newThread())
            .subscribe(this::handleGenericMessage));
  }

  @Override
  public Completable catchup(Channel channel) {
    Log.d(TAG, "sending a catchup to the channel " + channel);
    Catchup catchup = new Catchup(channel, requestCounter.incrementAndGet());
    return request(catchup)
        .map(ResultMessages.class::cast)
        .map(ResultMessages::getMessages)
        .doOnSuccess(messages -> Log.d(TAG, "Catchup on " + channel + " retrieved : " + messages))
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
    return request(publish).ignoreElement();
  }

  @Override
  public Completable subscribe(Channel channel) {
    Log.d(TAG, "sending a subscribe on the channel " + channel);
    Subscribe subscribe = new Subscribe(channel, requestCounter.incrementAndGet());
    return request(subscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(answer -> subscribedChannels.add(channel))
        .doAfterSuccess(answer -> catchup(channel))
        .ignoreElement();
  }

  @Override
  public Completable unsubscribe(Channel channel) {
    Log.d(TAG, "sending an unsubscribe on the channel " + channel);
    Unsubscribe unsubscribe = new Unsubscribe(channel, requestCounter.incrementAndGet());
    return request(unsubscribe)
        // This is used when reconnecting after a lost connection
        .doOnSuccess(answer -> subscribedChannels.remove(channel))
        .ignoreElement();
  }

  private void handleGenericMessage(GenericMessage genericMessage) {
    Log.d(TAG, "handling generic msg : " + genericMessage);
    if (genericMessage instanceof Broadcast) {
      // We've a Broadcast
      Broadcast broadcast = (Broadcast) genericMessage;
      try {
        messageHandler.handleMessage(
            repository, this, broadcast.getChannel(), broadcast.getMessage());
      } catch (DataHandlingException e) {
        Log.e(TAG, "Error while handling received message", e);
        unprocessed.onNext(genericMessage);
      }
    }
  }

  private void handleMessages(List<MessageGeneral> messages, Channel channel) {
    for (MessageGeneral msg : messages) {
      try {
        messageHandler.handleMessage(repository, this, channel, msg);
      } catch (DataHandlingException e) {
        Log.e(TAG, "Error while handling received catchup message", e);
      }
    }
  }

  private Single<Answer> request(Query query) {
    Single<Answer> answerSingle =
        connection
            .observeMessage() // Observe incoming messages
            .filter(Answer.class::isInstance) // Filter the Answers
            .map(Answer.class::cast)
            .filter(
                answer ->
                    answer.getId()
                        == query
                            .getRequestId()) // This specific request has an id, only let the
                                             // related Answer pass
            .doOnNext(answer -> Log.d(TAG, "request id: " + answer.getId()))
            // Transform from an Observable to a Single
            // This Means that we expect a result before the source is disposed and an error will be
            // produced if no value is received.
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
    // Only send the message after the single is created to make sure it is already waiting
    // before the answer is received
    connection.sendMessage(query);
    return answerSingle;
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
