package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.WebSocket;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

/** Represents a single websocket connection that can be closed */
public class Connection {

  private final LAOService laoService;
  private final BehaviorSubject<Lifecycle.State> manualState;

  public Connection(LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.laoService = laoService;
    this.manualState = manualState;
  }

  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }

  public Observable<GenericMessage> observeMessage() {
    return laoService.observeMessage();
  }

  public Observable<WebSocket.Event> observeConnectionEvents() {
    return laoService.observeWebsocket();
  }

  public void close() {
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
