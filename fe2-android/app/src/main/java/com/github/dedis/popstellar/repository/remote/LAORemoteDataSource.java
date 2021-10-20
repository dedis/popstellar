package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.repository.LAODataSource.Remote;
import com.tinder.scarlet.WebSocket;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;

public class LAORemoteDataSource implements Remote {

  private LAOService laoService;

  private AtomicInteger requestId;

  private static LAORemoteDataSource INSTANCE;

  private LAORemoteDataSource(LAOService service) {
    this.laoService = service;
    requestId = new AtomicInteger();
  }

  public static LAORemoteDataSource getInstance(LAOService laoService) {
    if (INSTANCE == null) {
      INSTANCE = new LAORemoteDataSource(laoService);
    }
    return INSTANCE;
  }

  public Observable<GenericMessage> observeMessage() {
    return laoService.observeMessage();
  }

  public Observable<WebSocket.Event> observeWebsocket() {
    return laoService.observeWebsocket();
  }

  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }

  public int incrementAndGetRequestId() {
    return requestId.incrementAndGet();
  }

  public int getRequestId() {
    return requestId.get();
  }
}
