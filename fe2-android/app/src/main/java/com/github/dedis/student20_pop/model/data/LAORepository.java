package com.github.dedis.student20_pop.model.data;

import android.util.Base64;
import androidx.annotation.NonNull;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Error;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Catchup;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.Unsubscribe;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.StateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall.StartType;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.OpenRollCall;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LAORepository {
  private static volatile LAORepository INSTANCE = null;

  private final LAODataSource.Remote mRemoteDataSource;
  private final LAODataSource.Local mLocalDataSource;

  // State for LAO
  private Map<String, Lao> laoById;

  // State for Messages
  private Map<String, MessageGeneral> messageById;

  // Outstanding subscribes
  private Map<Integer, String> subscribeRequests;

  // Outstanding catchups
  private Map<Integer, String> catchupRequests;

  private LAORepository(
      @NonNull LAODataSource.Remote remoteDataSource,
      @NonNull LAODataSource.Local localDataSource) {
    mRemoteDataSource = remoteDataSource;
    mLocalDataSource = localDataSource;

    laoById = new HashMap<>();
    messageById = new HashMap<>();
    subscribeRequests = new HashMap<>();
    catchupRequests = new HashMap<>();

    startSubscription();
  }

  private void startSubscription() {
    mRemoteDataSource
        .observeMessage()
        .subscribeOn(Schedulers.io())
        .subscribe(this::handleGenericMessage);
  }

  private void handleGenericMessage(GenericMessage genericMessage) {
    if (genericMessage instanceof Error) {
      Error err = (Error) genericMessage;
      int id = err.getId();
      if (subscribeRequests.containsKey(id)) {
        subscribeRequests.remove(id);
      } else if (catchupRequests.containsKey(id)) {
        catchupRequests.remove(id);
      }
      return;
    }

    if (genericMessage instanceof Result) {
      Result result = (Result) genericMessage;

      int id = result.getId();
      if (subscribeRequests.containsKey(id)) {
        String channel = subscribeRequests.get(id);
        subscribeRequests.remove(id);

        laoById.put(channel, new Lao(channel));
        // TODO: publish to LaoList subject

        sendCatchup(channel);
      } else if (catchupRequests.containsKey(id)) {
        String channel = catchupRequests.get(id);
        catchupRequests.remove(id);

        List<MessageGeneral> messages = result.getMessages().orElse(new ArrayList<>());
        for (MessageGeneral msg : messages) {
          handleMessage(channel, msg);
        }
      }

      return;
    }

    // We've a Broadcast
    Broadcast broadcast = (Broadcast) genericMessage;
    MessageGeneral message = broadcast.getMessage();
    String channel = broadcast.getChannel();

    handleMessage(channel, message);
    return;
  }

  private void handleMessage(String channel, MessageGeneral message) {
    // Put the message in the state
    messageById.put(message.getMessageId(), message);

    String senderPk = message.getSender();

    Data data = message.getData();
    if (data instanceof UpdateLao) {
      handleUpdateLao(channel, (UpdateLao) data);
    } else if (data instanceof StateLao) {
      handleStateLao(channel, (StateLao) data);
    } else if (data instanceof CreateRollCall) {
      handleCreateRollCall(channel, (CreateRollCall) data);
    } else if (data instanceof OpenRollCall) {
      handleOpenRollCall(channel, (OpenRollCall) data);
    } else if (data instanceof CloseRollCall) {
      handleCloseRollCall(channel, (CloseRollCall) data);
    } else if (data instanceof WitnessMessage) {
      handleWitnessMessage(channel, senderPk, (WitnessMessage) data);
    }
  }

  public void handleUpdateLao(String channel, UpdateLao updateLao) {}

  public void handleStateLao(String channel, StateLao stateLao) {}

  public void handleCreateRollCall(String channel, CreateRollCall createRollCall) {
    Lao lao = laoById.get(channel);

    Lao.RollCall rollCall = new Lao.RollCall();
    rollCall.setId(createRollCall.getId());
    rollCall.setCreation(createRollCall.getCreation());

    if (createRollCall.getStartType() == StartType.NOW) {
      rollCall.setStart(createRollCall.getStartTime());
    } else {
      rollCall.setScheduled(createRollCall.getStartTime());
    }

    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setDescription(createRollCall.getDescription().orElse(""));

    lao.updateRollCall(rollCall.getId(), rollCall);
  }

  public void handleOpenRollCall(String channel, OpenRollCall openRollCall) {
    Lao lao = laoById.get(channel);

    // TODO: get roll call if it exists and update the time. Make sure to set end = 0;
  }

  public void handleCloseRollCall(String channel, CloseRollCall closeRollCall) {
    Lao lao = laoById.get(channel);

    // TODO: get roll call if it exists and update the end time and attendees
  }

  public void handleWitnessMessage(String channel, String senderPk, WitnessMessage message) {
    String messageId = message.getMessageId();
    String signature = message.getSignature();

    byte[] senderPkBuf = Base64.decode(senderPk, Base64.NO_WRAP);
    byte[] signatureBuf = Base64.decode(signature, Base64.NO_WRAP);

    // Verify signature
    try {
      PublicKeyVerify verifier = new Ed25519Verify(senderPkBuf);
      verifier.verify(signatureBuf, Base64.decode(messageId, Base64.NO_WRAP));
    } catch (GeneralSecurityException e) {
      System.out.println("failed to verify witness signature " + e.getMessage());
      return;
    }

    if (messageById.containsKey(messageId)) {
      // Update the message
      MessageGeneral msg = messageById.get(messageId);
      msg.getWitnessSignatures().add(new PublicKeySignaturePair(senderPkBuf, signatureBuf));
      return;
    }

    // TODO: add the message back to the queue for later processing
  }

  public static LAORepository getInstance(
      LAODataSource.Remote laoRemoteDataSource, LAODataSource.Local localDataSource) {
    if (INSTANCE == null) {
      synchronized (LAORepository.class) {
        if (INSTANCE == null) {
          INSTANCE = new LAORepository(laoRemoteDataSource, localDataSource);
        }
      }
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    INSTANCE = null;
  }

  public Single<Answer> sendCatchup(String channel) {
    int id = mRemoteDataSource.incrementAndGetRequestId();
    Catchup catchup = new Catchup(channel, id);

    mRemoteDataSource.sendMessage(catchup);
    catchupRequests.put(id, channel);
    return createSingle(id);
  }

  public Single<Answer> sendPublish(String channel, MessageGeneral message) {
    int id = mRemoteDataSource.incrementAndGetRequestId();

    Publish publish = new Publish(channel, id, message);

    mRemoteDataSource.sendMessage(publish);
    return createSingle(id);
  }

  public Single<Answer> sendSubscribe(String channel) {
    int id = mRemoteDataSource.incrementAndGetRequestId();

    Subscribe subscribe = new Subscribe(channel, id);

    mRemoteDataSource.sendMessage(subscribe);
    subscribeRequests.put(id, channel);
    return createSingle(id);
  }

  public Single<Answer> sendUnsubscribe(String channel) {
    int id = mRemoteDataSource.incrementAndGetRequestId();

    Unsubscribe unsubscribe = new Unsubscribe(channel, id);

    mRemoteDataSource.sendMessage(unsubscribe);
    return createSingle(id);
  }

  private Single<Answer> createSingle(int id) {
    return mRemoteDataSource
        .observeMessage()
        .filter(
            genericMessage ->
                genericMessage instanceof Answer && ((Answer) genericMessage).getId() == id)
        .map(
            genericMessage -> {
              System.out.println(
                  "createSingle: Running on threadid=" + Thread.currentThread().getId());
              return (Answer) genericMessage;
            })
        .firstOrError();
  }

  // interactions with the state

  // createlao - put new lao in map
  // updatelao - update lao field
  // create roll call -
}
