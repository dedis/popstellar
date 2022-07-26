package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.*;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

/** Represents a single websocket connection that can be closed */
public class Connection {

  // Create a new subject whose purpose is to dispatch incoming messages to all subscribers
  private final BehaviorSubject<GenericMessage> messagesSubject = BehaviorSubject.create();
  private final BehaviorSubject<Lifecycle.State> manualState;
  private final LAOService laoService;
  private final Disposable disposable;

  public Connection(LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.laoService = laoService;
    // Subscribe to the incoming messages of the websocket service and simply hand them to the
    // subject
    this.disposable =
        laoService.observeMessage().subscribe(messagesSubject::onNext, messagesSubject::onError);
    this.manualState = manualState;
  }

  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }

  public Observable<GenericMessage> observeMessage() {
    return messagesSubject;
  }

  public Observable<WebSocket.Event> observeConnectionEvents() {
    return laoService.observeWebsocket();
  }

  public void close() {
    disposable.dispose();
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
