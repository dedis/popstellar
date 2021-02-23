package com.github.dedis.student20_pop.model.data;

import androidx.annotation.NonNull;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Catchup;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.Unsubscribe;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class LAORepository {
  private static volatile LAORepository INSTANCE = null;

  private final LAODataSource.Remote mRemoteDataSource;
  private final LAODataSource.Local mLocalDataSource;

  // State for LAO

  // State for Messages

  private LAORepository(
      @NonNull LAODataSource.Remote remoteDataSource,
      @NonNull LAODataSource.Local localDataSource) {
    mRemoteDataSource = remoteDataSource;
    mLocalDataSource = localDataSource;

    startSubscription();
  }

  private void startSubscription() {
    mRemoteDataSource
        .observeMessage()
        .subscribeOn(Schedulers.io())
        .subscribe(this::handleGenericMessage);
  }

  private void handleGenericMessage(GenericMessage genericMessage) {
    System.out.println("LAORepository::handleGenericMessage - received a message");
    if (genericMessage instanceof Result) {
      Result result = (Result) genericMessage;

      //      MessageGeneral[] messages = result.getMessages();

      //      for (MessageGeneral message : messages) {
      //        handleMessage(message);
      //      }
    }
  }

  private void handleMessage(MessageGeneral message) {
    Data data = message.getData();
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
