package com.github.dedis.student20_pop.utility.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.dedis.student20_pop.model.Person;

import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * A client endpoint to a web socket using jsr 365 library
 * <p>
 * TODO link onOpen and onRemove to the UI
 */
@ClientEndpoint()
public final class PoPClientEndpoint {

    private static final String TAG = PoPClientEndpoint.class.getName();

    private static final ClientManager client = ClientManager.createClient();
    private static final Map<Session, LowLevelClientProxy> listeners = new HashMap<>();

    /**
     * Create asynchronously a new HighLevelClientProxy that will encapsulate the socket
     *
     * @param host  to connect to
     * @param owner the person whose device issued the connection
     * @return A proxy that will be able to handle every high level tasks
     */
    public static HighLevelClientProxy connect(URI host, Person owner) {
        return new HighLevelClientProxy(owner, new LowLevelClientProxy(host));
    }


    /**
     * Create a new session with the websocket server
     *
     * @param host  to connect to
     * @param proxy that is issuing the connection
     * @return a completable future holding the session. If a fail occurs, the proxy will be responsible
     */
    protected static CompletableFuture<Session> connect(URI host, LowLevelClientProxy proxy) {
        CompletableFuture<Session> sessionFuture = new CompletableFuture<>();

        Thread t = new Thread(() -> {
            Looper.prepare();
            try {
                Session session = client.connectToServer(PoPClientEndpoint.class, host);
                registerProxy(session, proxy);
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

    private static void registerProxy(Session session, LowLevelClientProxy proxy) {
        synchronized (listeners) {
            listeners.put(session, proxy);
        }
    }

    public static void startPurgeRoutine(Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    listeners.values().forEach(LowLevelClientProxy::purge);
                    handler.postDelayed(this, LowLevelClientProxy.TIMEOUT);
                }
            }
        });
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
            LowLevelClientProxy client = listeners.get(session);
            if (client == null)
                throw new IllegalArgumentException();

            client.onMessage(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        synchronized (listeners) {
            listeners.remove(session);
        }
    }
}
