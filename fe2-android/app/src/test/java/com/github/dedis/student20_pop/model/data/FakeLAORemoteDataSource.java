package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.tinder.scarlet.WebSocket.Event;
import io.reactivex.Observable;

public class FakeLAORemoteDataSource implements LAODataSource.Remote {

  private static FakeLAORemoteDataSource INSTANCE;

  private FakeLAORemoteDataSource() {
  }

  public static FakeLAORemoteDataSource getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FakeLAORemoteDataSource();
    }
    return INSTANCE;
  }

  @Override
  public Observable<GenericMessage> observeMessage() {
    return null;
  }

  @Override
  public Observable<Event> observeWebsocket() {
    return null;
  }

  @Override
  public void sendMessage(Message msg) {

  }

  @Override
  public int getRequestId() {
    return 0;
  }

  @Override
  public int incrementAndGetRequestId() {
    return 0;
  }
}
