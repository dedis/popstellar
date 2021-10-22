package com.github.dedis.popstellar.repository;

import static com.github.dedis.popstellar.utility.handler.GenericHandler.handleBroadcast;
import static com.github.dedis.popstellar.utility.handler.GenericHandler.handleCatchup;
import static com.github.dedis.popstellar.utility.handler.GenericHandler.handleCreateLao;
import static com.github.dedis.popstellar.utility.handler.GenericHandler.handleError;
import static com.github.dedis.popstellar.utility.handler.GenericHandler.handleSubscribe;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Catchup;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.github.dedis.popstellar.model.network.method.Unsubscribe;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();
  private static final String ROOT = "/root/";
  private static LAORepository INSTANCE = null;

  @SuppressWarnings({"Implementation of LAOLocalDataSource is not complete.", "FieldCanBeLocal"})
  private final LAODataSource.Local mLocalDataSource;

  private final LAODataSource.Remote mRemoteDataSource;
  private final AndroidKeysetManager mKeysetManager;
  private final SchedulerProvider schedulerProvider;
  private final Gson mGson;

  // A subject that represents unprocessed messages
  private final Subject<GenericMessage> unprocessed;

  // State for LAO
  private final Map<String, LAOState> laoById;

  // State for Messages
  private final Map<String, MessageGeneral> messageById;

  // Outstanding subscribes
  private final Map<Integer, String> subscribeRequests;

  // set of subscribed channels
  private final Set<String> subscribedChannels;

  // Outstanding catchups
  private final Map<Integer, String> catchupRequests;

  // Outstanding create laos
  private final Map<Integer, String> createLaoRequests;

  // Observable for view models that need access to all LAO Names
  private final BehaviorSubject<List<Lao>> allLaoSubject;

  // Observable to subscribe to LAOs on reconnection
  private final Observable<WebSocket.Event> websocketEvents;

  // Observable to subscribe to the incoming messages
  private final Observable<GenericMessage> upstream;

  // Disposable of with the lifetime of an LAORepository instance
  private final Disposable disposables;

  private LAORepository(
      @NonNull LAODataSource.Remote remoteDataSource,
      @NonNull LAODataSource.Local localDataSource,
      @NonNull AndroidKeysetManager keysetManager,
      @NonNull Gson gson,
      @NonNull SchedulerProvider schedulerProvider) {
    mRemoteDataSource = remoteDataSource;
    mLocalDataSource = localDataSource;
    mKeysetManager = keysetManager;
    mGson = gson;

    laoById = new HashMap<>();
    messageById = new HashMap<>();
    subscribeRequests = new HashMap<>();
    catchupRequests = new HashMap<>();
    createLaoRequests = new HashMap<>();

    unprocessed = PublishSubject.create();

    allLaoSubject = BehaviorSubject.create();

    upstream = mRemoteDataSource.observeMessage().share();

    subscribedChannels = new HashSet<>();
    websocketEvents = mRemoteDataSource.observeWebsocket();

    this.schedulerProvider = schedulerProvider;

    // subscribe to incoming messages and the unprocessed message queue
    disposables = new CompositeDisposable(subscribeToUpstream(), subscribeToWebsocketEvents());
  }

  public static synchronized LAORepository getInstance(
      LAODataSource.Remote laoRemoteDataSource,
      LAODataSource.Local localDataSource,
      AndroidKeysetManager keysetManager,
      Gson gson,
      SchedulerProvider schedulerProvider) {
    if (INSTANCE == null) {
      INSTANCE =
          new LAORepository(
              laoRemoteDataSource, localDataSource, keysetManager, gson, schedulerProvider);
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    if (INSTANCE != null) {
      INSTANCE.dispose();
      INSTANCE = null;
    }
  }

  private Disposable subscribeToWebsocketEvents() {
    return websocketEvents
        .subscribeOn(schedulerProvider.io())
        .filter(event -> event.getClass().equals(WebSocket.Event.OnConnectionOpened.class))
        .subscribe(event -> subscribedChannels.forEach(this::sendSubscribe));
  }

  private Disposable subscribeToUpstream() {
    // We add a delay of 5 seconds to unprocessed messages to allow incoming messages to have a
    // higher priority
    return Observable.merge(
            upstream, unprocessed.delay(5, TimeUnit.SECONDS, schedulerProvider.computation()))
        .subscribeOn(schedulerProvider.newThread())
        .subscribe(this::handleGenericMessage);
  }

  private void handleGenericMessage(GenericMessage genericMessage) {
    Log.d(TAG, "handling generic msg");
    if (genericMessage instanceof Error) {
      handleError(genericMessage, subscribeRequests, catchupRequests, createLaoRequests);
      return;
    }

    if (genericMessage instanceof Result) {
      Result result = (Result) genericMessage;
      int id = result.getId();
      Log.d(TAG, "request id " + id);
      if (subscribeRequests.containsKey(id)) {
        handleSubscribe(this, id, subscribeRequests);
      } else if (catchupRequests.containsKey(id)) {
        handleCatchup(this, id, result, catchupRequests, unprocessed);
      } else if (createLaoRequests.containsKey(id)) {
        handleCreateLao(this, id, createLaoRequests);
      }
      return;
    }

    Log.d(TAG, "handleGenericMessage: got a broadcast");

    // We've a Broadcast
    handleBroadcast(this, genericMessage, unprocessed);
  }

  /**
   * Helper method that sends a StateLao message if we are the organizer
   *
   * @param lao Lao of the message being signed
   * @param msg Object of type MessageGeneral representing the current message being signed
   * @param messageId Base 64 URL encoded Id of the message to sign
   * @param channel Represents the channel on which to send the stateLao message
   */
  public void sendStateLao(Lao lao, MessageGeneral msg, String messageId, String channel) {
    try {
      KeysetHandle handle = mKeysetManager.getKeysetHandle().getPublicKeysetHandle();
      String ourKey = Keys.getEncodedKey(handle);
      if (ourKey.equals(lao.getOrganizer())) {
        UpdateLao updateLao = (UpdateLao) msg.getData();
        StateLao stateLao =
            new StateLao(
                lao.getId(),
                updateLao.getName(),
                lao.getCreation(),
                updateLao.getLastModified(),
                lao.getOrganizer(),
                messageId,
                updateLao.getWitnesses(),
                msg.getWitnessSignatures());

        byte[] ourPkBuf = Base64.getUrlDecoder().decode(ourKey);
        PublicKeySign signer = mKeysetManager.getKeysetHandle().getPrimitive(PublicKeySign.class);
        MessageGeneral stateLaoMsg = new MessageGeneral(ourPkBuf, stateLao, signer, mGson);

        sendPublish(channel, stateLaoMsg);
      }
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to get keyset handle: " + e.getMessage());
    } catch (IOException e) {
      Log.d(TAG, "failed to get encoded public key: " + e.getMessage());
    }
  }

  public Single<Answer> sendCatchup(String channel) {
    Log.d(TAG, "sending a catchup to the channel " + channel);
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Catchup catchup = new Catchup(channel, id);

    catchupRequests.put(id, channel);
    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(catchup);
    return answer;
  }

  public Single<Answer> sendPublish(String channel, MessageGeneral message) {
    Log.d(TAG, "sending a publish " + message.getData().getClass() + " to the channel " + channel);
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Single<Answer> answer = createSingle(id);

    Publish publish = new Publish(channel, id, message);
    if (message.getData() instanceof CreateLao) {
      CreateLao data = (CreateLao) message.getData();
      createLaoRequests.put(id, ROOT + data.getId());
    }

    mRemoteDataSource.sendMessage(publish);
    return answer;
  }

  public Single<Answer> sendSubscribe(String channel) {
    Log.d(TAG, "sending a subscribe to the channel " + channel);
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Subscribe subscribe = new Subscribe(channel, id);

    subscribeRequests.put(id, channel);

    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(subscribe);

    subscribedChannels.add(channel);

    return answer;
  }

  public Single<Answer> sendUnsubscribe(String channel) {
    Log.d(TAG, "sending an unsubscribe to the channel " + channel);
    int id = mRemoteDataSource.incrementAndGetRequestId();

    Unsubscribe unsubscribe = new Unsubscribe(channel, id);

    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(unsubscribe);
    return answer;
  }

  /**
   * Helper method that looks for the Answer of the given id and creates a Single
   *
   * @param id of the answer
   * @return a single answer
   */
  private Single<Answer> createSingle(int id) {
    return upstream
        .filter(
            genericMessage -> {
              if (genericMessage instanceof Answer) {
                Log.d(TAG, "request id: " + ((Answer) genericMessage).getId());
              }
              return genericMessage instanceof Answer && ((Answer) genericMessage).getId() == id;
            })
        .map(Answer.class::cast)
        .firstOrError()
        .subscribeOn(schedulerProvider.io())
        .cache();
  }

  /**
   * Checks that a given channel corresponds to a LAO channel, i.e /root/laoId
   *
   * @param channel the channel we want to check
   * @return true if the channel is a lao channel, false otherwise
   */
  public boolean isLaoChannel(String channel) {
    return channel.split("/").length == 3;
  }

  /** Set allLaoSubject to contain all LAOs */
  public void setAllLaoSubject() {
    Log.d(TAG, "posted allLaos to allLaoSubject");
    allLaoSubject.onNext(
        laoById.entrySet().stream().map(x -> x.getValue().getLao()).collect(Collectors.toList()));
  }

  /**
   * Retrieves the Election in a given channel
   *
   * @param channel the channel on which the election was created
   * @return the election corresponding to this channel
   */
  public Election getElectionByChannel(String channel) {
    Log.d(TAG, "querying election for channel " + channel);

    if (channel.split("/").length < 4)
      throw new IllegalArgumentException("invalid channel for an election : " + channel);

    Lao lao = getLaoByChannel(channel);
    Optional<Election> electionOption = lao.getElection(channel.split("/")[3]);
    if (!electionOption.isPresent()) {
      throw new IllegalArgumentException("the election should be present when receiving a result");
    }
    return electionOption.get();
  }

  /**
   * Retrieves the Lao in a given channel
   *
   * @param channel the channel on which the Lao was created
   * @return the Lao corresponding to this channel
   */
  public Lao getLaoByChannel(String channel) {
    Log.d(TAG, "querying lao for channel " + channel);

    String[] split = channel.split("/");
    return laoById.get(ROOT + split[2]).getLao();
  }

  public Observable<List<Lao>> getAllLaos() {
    return allLaoSubject;
  }

  public Observable<Lao> getLaoObservable(String channel) {
    Log.d(TAG, "LaoIds we have are: " + laoById.keySet());
    return laoById.get(channel).getObservable();
  }

  public Map<String, LAOState> getLaoById() {
    return laoById;
  }

  public Map<String, MessageGeneral> getMessageById() {
    return messageById;
  }

  private void dispose() {
    disposables.dispose();
  }
}
