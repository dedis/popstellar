package com.github.dedis.popstellar.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.repository.remote.LAOService;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.WebSocket;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class Connection implements LAOService {

  private final LAOService laoService;
  private final BehaviorSubject<Lifecycle.State> manualState;

  public Connection(LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.laoService = laoService;
    this.manualState = manualState;
  }

  @Override
  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }

  @Override
  public Observable<GenericMessage> observeMessage() {
    return laoService.observeMessage();
  }

  @Override
  public Observable<WebSocket.Event> observeWebsocket() {
    return laoService.observeWebsocket();
  }

  public void close() {
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
