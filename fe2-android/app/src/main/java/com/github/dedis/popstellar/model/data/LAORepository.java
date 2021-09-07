package com.github.dedis.popstellar.model.data;

import android.util.Log;
import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.Election;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.data.handler.GeneralHandler;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.Catchup;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.github.dedis.popstellar.model.network.method.Unsubscribe;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();
  private static LAORepository INSTANCE = null;

  private final LAODataSource.Remote mRemoteDataSource;
  private final LAODataSource.Local mLocalDataSource;
  private final AndroidKeysetManager mKeysetManager;
  private final Gson mGson;
  private final SchedulerProvider schedulerProvider;

  // A subject that represents unprocessed messages
  private Subject<GenericMessage> unprocessed;

  // State for LAO
  private Map<String, LAOState> laoById;

  // State for Messages
  private Map<String, MessageGeneral> messageById;

  // Outstanding subscribes
  private Map<Integer, String> subscribeRequests;

  private Set<String> subscribedChannels;

  // Outstanding catchups
  private Map<Integer, String> catchupRequests;

  // Outstanding create laos
  private Map<Integer, String> createLaoRequests;

  // Observable for view models that need access to all LAO Names
  private BehaviorSubject<List<Lao>> allLaoSubject;

  // Observable to subscribe to LAOs on reconnection
  private Observable<WebSocket.Event> websocketEvents;

  private Observable<GenericMessage> upstream;

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
    startSubscription();
    subscribeToWebsocketEvents();
  }

  public static synchronized LAORepository getInstance(
      LAODataSource.Remote laoRemoteDataSource,
      LAODataSource.Local localDataSource,
      AndroidKeysetManager keysetManager,
      Gson gson, SchedulerProvider schedulerProvider) {
    if (INSTANCE == null) {
      INSTANCE = new LAORepository(laoRemoteDataSource, localDataSource, keysetManager, gson,
          schedulerProvider);
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    INSTANCE = null;
  }

  private void subscribeToWebsocketEvents() {
    websocketEvents
        .subscribeOn(schedulerProvider.io())
        .filter(event -> event.getClass().equals(WebSocket.Event.OnConnectionOpened.class))
        .subscribe(event -> subscribedChannels.forEach(this::sendSubscribe));
  }

  private void startSubscription() {
    // We add a delay of 5 seconds to unprocessed messages to allow incoming messages to have a
    // higher priority
    Observable
        .merge(upstream, unprocessed.delay(5, TimeUnit.SECONDS, schedulerProvider.computation()))
        .subscribeOn(schedulerProvider.newThread())
        .subscribe(this::handleGenericMessage);
  }

  // TODO: Create utility class to handle messages
  private void handleGenericMessage(GenericMessage genericMessage) {
    Log.d(TAG, "handling generic msg");
    if (genericMessage instanceof Error) {
      Error err = (Error) genericMessage;
      int id = err.getId();
      if (subscribeRequests.containsKey(id)) {
        subscribeRequests.remove(id);
      } else if (catchupRequests.containsKey(id)) {
        catchupRequests.remove(id);
      } else if (createLaoRequests.containsKey(id)) {
        createLaoRequests.remove(id);
      }
      return;
    }

    if (genericMessage instanceof Result) {
      Result result = (Result) genericMessage;

      int id = result.getId();
      Log.d(TAG, "handleGenericMessage: request id " + id);
      if (subscribeRequests.containsKey(id)) {
        String channel = subscribeRequests.get(id);
        subscribeRequests.remove(id);

        if (isLaoChannel(channel)) {
          Lao lao = new Lao(channel);
          laoById.put(channel, new LAOState(lao));
          allLaoSubject.onNext(
              laoById.entrySet().stream()
                  .map(x -> x.getValue().getLao())
                  .collect(Collectors.toList()));

          Log.d(TAG, "posted allLaos to `allLaoSubject`");
          sendCatchup(channel);
        }
      } else if (catchupRequests.containsKey(id)) {
        String channel = catchupRequests.get(id);
        catchupRequests.remove(id);

        Log.d(TAG, "got a catchup request in response to request id " + id);
        List<MessageGeneral> messages = result.getMessages().orElse(new ArrayList<>());
        Log.d(TAG, "messages length: " + messages.size());
        for (MessageGeneral msg : messages) {
          boolean enqueue = GeneralHandler.handleMessage(this, channel, msg);
          if (enqueue) {
            unprocessed.onNext(genericMessage);
          }
        }
      } else if (createLaoRequests.containsKey(id)) {
        String channel = createLaoRequests.get(id);
        createLaoRequests.remove(id);

        Lao lao = new Lao(channel);
        laoById.put(channel, new LAOState(lao));
        allLaoSubject.onNext(
            laoById.entrySet().stream()
                .map(x -> x.getValue().getLao())
                .collect(Collectors.toList()));
        Log.d(TAG, "createLaoRequest contains this id. posted allLaos to `allLaoSubject`");
        sendSubscribe(channel);
        sendCatchup(channel);
      }

      return;
    }

    Log.d(TAG, "Got a broadcast");

    // We've a Broadcast
    Broadcast broadcast = (Broadcast) genericMessage;
    MessageGeneral message = broadcast.getMessage();
    String channel = broadcast.getChannel();

    Log.d(TAG, "Broadcast channel: " + channel + " message " + message.getMessageId());

    boolean enqueue = GeneralHandler.handleMessage(this, channel, message);
    if (enqueue) {
      unprocessed.onNext(genericMessage);
    }
  }

  /**
   * // TODO: refactor Retrieves the Election in a given channel
   *
   * @param channel the channel on which the election was created
   * @return the election corresponding to this channel
   */
  public Election getElectionByChannel(String channel) {
    Lao lao = getLaoByChannel(channel);
    Optional<Election> electionOption = lao.getElection(channel.split("/")[3]);
    if (!electionOption.isPresent()) {
      throw new IllegalArgumentException("the election should be present when receiving a result");
    }
    return electionOption.get();
  }

  /**
   * TODO: refactor Retrieves the Lao in a given channel
   *
   * @param channel the channel on which the Lao was created
   * @return the Lao corresponding to this channel
   */
  public Lao getLaoByChannel(String channel) {
    String[] split = channel.split("/");
    return laoById.get("/root/" + split[2]).getLao();
  }

  // TODO: new getters for witness message handler
  public BehaviorSubject<List<Lao>> getAllLaoSubject() {
    return allLaoSubject;
  }

  public Map<String, LAOState> getLaoById() {
    return laoById;
  }

  /**
   * TODO: refactor
   */
  public Map<String, MessageGeneral> getMessageById() {
    return messageById;
  }

  /**
   * TODO: add to utility lao or message general handler Checks that a given channel corresponds to
   * a LAO channel, i.e /root/laoId
   *
   * @param channel the channel we want to check
   * @return true if the channel is a lao channel, false otherwise
   */
  public boolean isLaoChannel(String channel) {
    return channel.split("/").length == 3;
  }

  /**
   * TODO: make private? Helper method that sends a StateLao message if we are the organizer
   *
   * @param lao       Lao of the message being signed
   * @param msg       Object of type MessageGeneral representing the current message being signed
   * @param messageId Base 64 URL encoded Id of the message to sign
   * @param channel   Represents the channel on which to send the stateLao message
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
        PublicKeySign signer =
            mKeysetManager.getKeysetHandle().getPrimitive(PublicKeySign.class);
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
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Catchup catchup = new Catchup(channel, id);

    catchupRequests.put(id, channel);
    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(catchup);
    return answer;
  }

  public Single<Answer> sendPublish(String channel, MessageGeneral message) {
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Single<Answer> answer = createSingle(id);

    Publish publish = new Publish(channel, id, message);
    if (message.getData() instanceof CreateLao) {
      CreateLao data = (CreateLao) message.getData();
      createLaoRequests.put(id, "/root/" + data.getId());
    }

    mRemoteDataSource.sendMessage(publish);
    return answer;
  }

  public Single<Answer> sendSubscribe(String channel) {

    int id = mRemoteDataSource.incrementAndGetRequestId();

    Subscribe subscribe = new Subscribe(channel, id);

    subscribeRequests.put(id, channel);

    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(subscribe);
    Log.d(TAG, "sending subscribe");

    subscribedChannels.add(channel);

    return answer;
  }

  public Single<Answer> sendUnsubscribe(String channel) {
    int id = mRemoteDataSource.incrementAndGetRequestId();

    Unsubscribe unsubscribe = new Unsubscribe(channel, id);

    Single<Answer> answer = createSingle(id);
    mRemoteDataSource.sendMessage(unsubscribe);
    return answer;
  }

  private Single<Answer> createSingle(int id) {
    return upstream
        .filter(
            genericMessage -> {
              if (genericMessage instanceof Answer) {
                Log.d(TAG, "request id: " + ((Answer) genericMessage).getId());
              }
              return genericMessage instanceof Answer
                  && ((Answer) genericMessage).getId() == id;
            })
        .map(Answer.class::cast)
        .firstOrError()
        .subscribeOn(schedulerProvider.io())
        .cache();
  }

  public Observable<List<Lao>> getAllLaos() {
    return allLaoSubject;
  }

  public Observable<Lao> getLaoObservable(String channel) {
    Log.d(TAG, "LaoIds we have are: " + laoById.keySet());
    return laoById.get(channel).getObservable();
  }
}
