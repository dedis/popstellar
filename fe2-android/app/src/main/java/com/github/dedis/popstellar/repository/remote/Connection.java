package com.github.dedis.popstellar.repository.remote;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.*;
import com.tinder.scarlet.WebSocket.Event.*;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

/** Represents a single websocket connection that can be closed */
public class Connection {

  public static final String TAG = Connection.class.getSimpleName();

  // Create a new subject whose purpose is to dispatch incoming messages to all subscribers
  private final BehaviorSubject<GenericMessage> messagesSubject = BehaviorSubject.create();
  private final BehaviorSubject<Lifecycle.State> manualState;
  private final LAOService laoService;
  private final CompositeDisposable disposables = new CompositeDisposable();

  public Connection(
      String url, LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.laoService = laoService;
    // Subscribe to the incoming messages of the websocket service and simply hand them to the
    // subject
    this.disposables.add(
        laoService
            .observeMessage()
            .doOnNext(msg -> Log.d(TAG, "Received a new message from remote: " + msg))
            .subscribe(messagesSubject::onNext, messagesSubject::onError));

    // Add logs on connection stated
    disposables.add(
        this.laoService
            .observeWebsocket()
            .subscribe(
                event -> logEvent(event, url),
                err -> Log.d(TAG, "Error in connection " + url, err)));
    this.manualState = manualState;
  }

  private void logEvent(WebSocket.Event event, String url) {
    if (event instanceof OnConnectionOpened) {
      Log.d(TAG, "Connection to " + url + " opened");
    } else if (event instanceof OnConnectionClosed) {
      ShutdownReason reason = ((OnConnectionClosed) event).getShutdownReason();
      Log.d(TAG, "Connection to " + url + " closed: " + reason);
    } else if (event instanceof OnConnectionFailed) {
      Throwable error = ((OnConnectionFailed) event).getThrowable();
      Log.d(TAG, "Connection to " + url + " failed", error);
    } else if (event instanceof OnConnectionClosing) {
      ShutdownReason reason = ((OnConnectionClosing) event).getShutdownReason();
      Log.d(TAG, "Connection to " + url + " closing: " + reason);
    }
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
    disposables.dispose();
    messagesSubject.onComplete();
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
