package com.github.dedis.student20_pop.utility.network;

import android.os.Looper;
import android.util.Log;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A client endpoint to a web socket using jsr 365 library
 *
 * <p>TODO link onOpen and onRemove to the UI
 */
@ClientEndpoint()
public final class WebSocketEndpoint {

  private static final String TAG = WebSocketEndpoint.class.getName();

  private static final ClientManager client = ClientManager.createClient();
  private static final Map<Session, MessageListener> listeners = new HashMap<>();

  /**
   * Create a new session with the websocket server
   *
   * @param host to connect to
   * @param proxy that is issuing the connection
   * @return a completable future holding the session. If a fail occurs, the proxy will be
   *     responsible
   */
  protected static CompletableFuture<Session> connect(URI host, WebSocketLowLevelProxy proxy) {
    CompletableFuture<Session> sessionFuture = new CompletableFuture<>();

    Thread t =
        new Thread(
            () -> {
              Looper.prepare();
              try {
                Session session = client.connectToServer(WebSocketEndpoint.class, host);
                registerListener(session, proxy);
                sessionFuture.complete(session);
              } catch (DeploymentException e) {
                sessionFuture.completeExceptionally(e);
              }
              Looper.loop();
            });
    t.setDaemon(true);
    t.start();

    return sessionFuture;
  }

  private static void registerListener(Session session, MessageListener listener) {
    synchronized (listeners) {
      listeners.put(session, listener);
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    Log.i(TAG, "Client successfully connected to " + session.getId());
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    // Messages from a session are taken care one at the time to avoid any problem
    // In the future, we could improve this
    synchronized (listeners) {
      MessageListener listener = listeners.get(session);
      if (listener == null) throw new IllegalArgumentException();

      listener.onMessage(message);
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    synchronized (listeners) {
      listeners.remove(session);
    }
  }
}
