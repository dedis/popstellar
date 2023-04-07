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
  private final BehaviorSubject<GenericMessage> messagesSubject;
  private final BehaviorSubject<Lifecycle.State> manualState;
  private final LAOService laoService;
  private final CompositeDisposable disposables;

  public Connection(
      String url, LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.messagesSubject = BehaviorSubject.create();
    this.laoService = laoService;
    this.disposables = new CompositeDisposable();
    // Subscribe to the incoming messages of the websocket service
    // and simply hand them to the subject
    this.disposables.add(
        laoService
            .observeMessage()
            .doOnNext(msg -> Log.d(TAG, "Received a new message from remote: " + msg))
            .subscribe(messagesSubject::onNext, messagesSubject::onError));

    // Add logs on connection state events
    disposables.add(
        this.laoService
            .observeWebsocket()
            .subscribe(
                event -> logEvent(event, url),
                err -> Log.d(TAG, "Error in connection " + url, err)));
    this.manualState = manualState;
  }

  protected Connection(Connection connection) {
    this.laoService = connection.laoService;
    this.manualState = connection.manualState;
    this.disposables = connection.disposables;
    this.messagesSubject = connection.messagesSubject;
  }

  private void logEvent(WebSocket.Event event, String url) {
    String baseMsg = "Connection to " + url;

    if (event instanceof OnConnectionOpened) {
      Log.i(TAG, baseMsg + " opened");
    } else if (event instanceof OnConnectionClosed) {
      ShutdownReason reason = ((OnConnectionClosed) event).getShutdownReason();
      Log.i(TAG, baseMsg + " closed: " + reason);
    } else if (event instanceof OnConnectionFailed) {
      Throwable error = ((OnConnectionFailed) event).getThrowable();
      Log.d(TAG, baseMsg + " failed", error);
    } else if (event instanceof OnConnectionClosing) {
      ShutdownReason reason = ((OnConnectionClosing) event).getShutdownReason();
      Log.d(TAG, baseMsg + " is closing: " + reason);
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
    // Dispose of any held resources and mark the message subject as complete
    // (i.e. will not be used again)
    messagesSubject.onComplete();
    disposables.dispose();
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
