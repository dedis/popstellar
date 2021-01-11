package com.github.dedis.student20_pop.utility.network;

import android.util.Log;

import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.low.Broadcast;
import com.github.dedis.student20_pop.model.network.level.low.Catchup;
import com.github.dedis.student20_pop.model.network.level.low.Message;
import com.github.dedis.student20_pop.model.network.level.low.Publish;
import com.github.dedis.student20_pop.model.network.level.low.Query;
import com.github.dedis.student20_pop.model.network.level.low.Subscribe;
import com.github.dedis.student20_pop.model.network.level.low.Unsubscribe;
import com.github.dedis.student20_pop.model.network.level.low.answer.Answer;
import com.github.dedis.student20_pop.model.network.level.low.answer.Error;
import com.github.dedis.student20_pop.model.network.level.low.answer.Result;
import com.github.dedis.student20_pop.model.network.level.mid.MessageGeneral;
import com.github.dedis.student20_pop.utility.json.JsonUtils;
import com.github.dedis.student20_pop.utility.protocol.LowLevelProxy;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the publish-subscribe protocol
 */
public final class WebSocketLowLevelProxy implements LowLevelProxy, IMessageListener {

    private static final String TAG = WebSocketLowLevelProxy.class.getName();

    // Lock to prevent multiple threads to access the session
    private final Object SESSION_LOCK = new Object();

    private final URI sessionURI;
    private final Gson gson = JsonUtils.createGson();
    private final Map<Integer, RequestEntry> requests = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    // Having to different futures to be able to close the session even if it is closed during the opening.
    // The first future holds the session and it will immediately be passed to the second on completion
    private CompletableFuture<Session> session;
    // The second future is used to make requests.
    // If the connection is cancelled, this future complete exceptionally but future1 is kept clean
    // to be able to close the session as soon as it is created.
    private CompletableFuture<Session> sessionUse;

    public WebSocketLowLevelProxy(URI host) {
        this.sessionURI = host;
        this.session = WebSocketEndpoint.connect(host, this);
        this.sessionUse = session.thenApply(s -> s);
    }

    /**
     * Make a request to the connected session.
     * Generate a unique id and save the the CompletableFuture that will be complete once a response is received.
     *
     * @param responseType    the expected type of the response data
     * @param requestSupplier a generator that take as input the id of the request and output the actual request object
     * @param <T>             generic type of the expected response data
     * @return a CompletableFuture that will be completed when the response is received (or if it timeouts)
     */
    private <T> CompletableFuture<T> makeRequest(Class<T> responseType, Function<Integer, Query> requestSupplier) {
        RequestEntry entry = new RequestEntry();
        // Put the result to a new id, if it is already taken, generate a new id until it fits in.
        // Uses overflows to get back to the start.
        // It might create an infinite loop. But only if there are already 2^32 pending requests.
        // Which is very unlikely and the timeout system will take care of them
        int id;
        do {
            id = counter.incrementAndGet();
        } while (requests.putIfAbsent(id, entry) != null);

        Query query = requestSupplier.apply(id);
        String txt = gson.toJson(query, Message.class);
        // Refreshing the session if needed
        refreshSession();

        sessionUse.thenAccept(session -> {
            synchronized (SESSION_LOCK) {
                session.getAsyncRemote().sendText(txt);
            }
        }).exceptionally(t -> {
            entry.requests.completeExceptionally(t);
            return null; // Java cannot parse void to Void. So this is sadly needed
        });

        return entry.requests.thenApply(elem -> gson.fromJson(elem, responseType));
    }

    private void refreshSession() {
        synchronized (SESSION_LOCK) {
            if (session.isDone()) {
                Optional<Session> session = getSession();
                if (!session.isPresent() || !session.get().isOpen()) {
                    //There was an error during competition, retry
                    Log.d(TAG, "Connection to " + sessionURI + " was either lost or never made. Trying to reconnect...");
                    this.session = WebSocketEndpoint.connect(sessionURI, this);
                    sessionUse = this.session.thenApply(s -> s);
                }
            }
        }
    }

    @Override
    public CompletableFuture<Integer> subscribe(String channel) {
        return makeRequest(Integer.class, id -> new Subscribe(channel, id));
    }

    @Override
    public CompletableFuture<Integer> unsubscribe(String channel) {
        return makeRequest(Integer.class, id -> new Unsubscribe(channel, id));
    }


    @Override
    public CompletableFuture<Integer> publish(String sender, String key, String channel, Data message) {
        String data = Base64.getEncoder().encodeToString(gson.toJson(message, Data.class).getBytes(StandardCharsets.UTF_8));
        String signature = Signature.sign(key, data);
        String msgId = Hash.hash(data, signature);
        MessageGeneral container = new MessageGeneral(sender, data, signature, msgId, new ArrayList<>());
        return makeRequest(Integer.class, id -> new Publish(channel, id, container));
    }

    @Override
    public CompletableFuture<List<Data>> catchup(String channel) {
        CompletableFuture<Data[]> future = makeRequest(Data[].class, id -> new Catchup(channel, id));
        return future.thenApply(Arrays::asList);
    }

    @Override
    public void onMessage(String msg) {
        JsonObject obj = gson.fromJson(msg, JsonObject.class);
        //TODO not extremely happy about this
        if (obj.has("method")) {
            handleMessage(gson.fromJson(obj, Broadcast.class));
        } else {
            handleResult(gson.fromJson(obj, Answer.class));
        }
    }

    /**
     * Handles a received answer. Find tis matching request and complete it.
     *
     * @param answer received
     */
    private void handleResult(Answer answer) {
        RequestEntry entry = requests.remove(answer.getId());
        if (entry == null)
            throw new IllegalStateException("Received unknown Answer id");

        // There is a way this is the wrong Answer : if there was a timeout and the message came back while another was generated on the same id.
        // But this is a very rare case and we could add a timestamp to the protocol to fix this issue
        if (answer instanceof Result) {
            Result result = (Result) answer;
            entry.requests.complete(result.getResult());
        } else if (answer instanceof Error) {
            Error error = (Error) answer;
            entry.requests.completeExceptionally(new RuntimeException("Error code " + error.getError().getCode() + " : " + error.getError().getDescription()));
        } else {
            throw new IllegalArgumentException("Unknown result class");
        }
    }

    /**
     * Extract the high level message from the low level received message and handles it
     * <p>
     * TODO Handle the messages
     *
     * @param broadcast the low level message received
     */
    private void handleMessage(Broadcast broadcast) {
        MessageGeneral container = broadcast.getMessage();
        Data data = gson.fromJson(
                new String(
                        Base64.getDecoder().decode(container.getData()),
                        StandardCharsets.UTF_8),
                Data.class);

        System.out.println(data);
    }


    @Override
    public void purgeTimeoutRequests() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, RequestEntry>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            RequestEntry entry = it.next().getValue();
            if (currentTime - entry.timestamp > LowLevelProxy.REQUEST_TIMEOUT) {
                entry.requests.completeExceptionally(new TimeoutException("Query timeout"));
                it.remove();
            }
        }
    }

    private Optional<Session> getSession() {
        try {
            return Optional.ofNullable(sessionUse.getNow(null));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public void close(Throwable reason) {
        synchronized (SESSION_LOCK) {
            sessionUse.completeExceptionally(reason);

            session.thenAccept(s -> {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Complete all pending requests and remove them
        Iterator<Map.Entry<Integer, RequestEntry>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().requests.completeExceptionally(reason);
            it.remove();
        }
    }

    @Override
    public void close() {
        close(new IOException("Session closed"));
    }

    private final static class RequestEntry {

        private final long timestamp = System.currentTimeMillis();
        private final CompletableFuture<JsonElement> requests = new CompletableFuture<>();
    }
}
