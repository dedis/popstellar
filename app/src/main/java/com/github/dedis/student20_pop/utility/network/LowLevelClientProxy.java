package com.github.dedis.student20_pop.utility.network;

import android.util.Log;

import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.low.Catchup;
import com.github.dedis.student20_pop.model.network.level.low.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.level.low.LowLevelMessage;
import com.github.dedis.student20_pop.model.network.level.low.Publish;
import com.github.dedis.student20_pop.model.network.level.low.Request;
import com.github.dedis.student20_pop.model.network.level.low.Subscribe;
import com.github.dedis.student20_pop.model.network.level.low.Unsubscribe;
import com.github.dedis.student20_pop.model.network.level.low.result.Failure;
import com.github.dedis.student20_pop.model.network.level.low.result.Result;
import com.github.dedis.student20_pop.model.network.level.low.result.Success;
import com.github.dedis.student20_pop.model.network.level.mid.MessageContainer;
import com.github.dedis.student20_pop.utility.json.JsonUtils;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Closeable;
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
public final class LowLevelClientProxy implements Closeable {

    public static final long TIMEOUT = 5000L; // 5 secs

    private static final String TAG = LowLevelClientProxy.class.getName();

    // Lock to prevent multiple threads to access the session
    private final Object SESSION_LOCK = new Object();

    private final URI sessionURI;
    // Having to different futures to be able to close the session even if it is closed during the opening.
    // The first future holds the session and it will immediately be passed to the second on completion
    private CompletableFuture<Session> future1;
    // The second future is used to make requests.
    // If the connection is cancelled, this future complete exceptionally but future1 is kept clean
    // to be able to close the session as soon as it is created.
    private CompletableFuture<Session> future2;

    private final Gson gson = JsonUtils.createGson();
    private final Map<Integer, RequestEntry> requests = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    public LowLevelClientProxy(URI host) {
        this.sessionURI = host;
        this.future1 = PoPClientEndpoint.connect(host, this);
        this.future2 = future1.thenApply(s -> s);
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
    private <T> CompletableFuture<T> makeRequest(Class<T> responseType, Function<Integer, Request> requestSupplier) {
        RequestEntry entry = new RequestEntry();
        // Put the result to a new id, if it is already taken, generate a new id until it fits in.
        // Uses overflows to get back to the start.
        // It might create an infinite loop. But only if there are already 2^32 pending requests.
        // Which is very unlikely and the timeout system will take care of them
        int id;
        do {
            id = counter.incrementAndGet();
        } while (requests.putIfAbsent(id, entry) != null);

        Request request = requestSupplier.apply(id);
        String txt = gson.toJson(request, ChanneledMessage.class);
        // Refreshing the session if needed
        refreshSession();

        future2.thenAccept(session -> {
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
            if(future1.isDone()) {
                Optional<Session> session = getSession();
                if (!session.isPresent() || !session.get().isOpen()) {
                    //There was an error during competition, retry
                    Log.d(TAG, "Connection to " + sessionURI + " was either lost or never made. Trying to reconnect...");
                    future1 = PoPClientEndpoint.connect(sessionURI, this);
                    future2 = future1.thenApply(s -> s);
                }
            }
        }
    }

    /**
     * Subscribe to a channel
     *
     * @param channel to subscribe to
     * @return a completable future holding the response value
     */
    public CompletableFuture<Integer> subscribe(String channel) {
        return makeRequest(Integer.class, id -> new Subscribe(channel, id));
    }

    /**
     * Unsubscribe to a channel
     *
     * @param channel to subscribe to
     * @return a completable future holding the response value
     */
    public CompletableFuture<Integer> unsubscribe(String channel) {
        return makeRequest(Integer.class, id -> new Unsubscribe(channel, id));
    }

    /**
     * Publish an event on given channel
     *
     * @param channel to publish the event on
     * @param message to publish
     * @return a completable future holding the response value
     */
    public CompletableFuture<Integer> publish(String sender, String key, String channel, Message message) {
        String data = Base64.getEncoder().encodeToString(gson.toJson(message, Message.class).getBytes(StandardCharsets.UTF_8));
        String signature = Signature.sign(key, data);
        String msgId = Hash.hash(data + signature);
        MessageContainer container = new MessageContainer(sender, data, signature, msgId, new ArrayList<>());
        return makeRequest(Integer.class, id -> new Publish(channel, id, container));
    }

    /**
     * Catchup on missed messages from a given channel
     *
     * @param channel to fetch from
     */
    public CompletableFuture<List<Message>> catchup(String channel) {
        CompletableFuture<Message[]> future = makeRequest(Message[].class, id -> new Catchup(channel, id));
        return future.thenApply(Arrays::asList);
    }

    /**
     * Called by the underlying socket endpoint when a message is received
     *
     * @param msg received
     */
    void onMessage(String msg) {
        JsonObject obj = gson.fromJson(msg, JsonObject.class);
        if (obj.has("method")) {
            handleMessage(gson.fromJson(obj, LowLevelMessage.class));
        } else {
            handleResult(gson.fromJson(obj, Result.class));
        }
    }

    /**
     * Handles a received response. Find tis matching request and complete it.
     *
     * @param result received
     */
    private void handleResult(Result result) {
        RequestEntry entry = requests.remove(result.getId());
        if (entry == null)
            throw new Error("Received unknown result id");


        // There is a way this is the wrong answer : if there was a timeout and the message came back while another was generated on the same id.
        // But this is a very rare case and we could add a timestamp to the protocol to fix this issue
        if (result instanceof Success) {
            Success success = (Success) result;
            entry.requests.complete(success.getResult());
        } else if (result instanceof Failure) {
            Failure failure = (Failure) result;
            entry.requests.completeExceptionally(new RuntimeException("Error code " + failure.getError().getCode() + " : " + failure.getError().getDescription()));
        } else {
            throw new Error("Unknown result class");
        }
    }

    /**
     * Extract the high level message from the low level received message and handles it
     * <p>
     * TODO Handle the messages
     *
     * @param lowLevelMessage the low level message received
     */
    private void handleMessage(LowLevelMessage lowLevelMessage) {
        MessageContainer container = lowLevelMessage.getMessage();
        Message message = gson.fromJson(
                new String(
                        Base64.getDecoder().decode(container.getData()),
                        StandardCharsets.UTF_8),
                Message.class);

        System.out.println(message);
    }

    /**
     * Lookup the request table and remove the request that have timed out.
     */
    public void purge() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, RequestEntry>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            RequestEntry entry = it.next().getValue();
            if (currentTime - entry.timestamp > TIMEOUT) {
                entry.requests.completeExceptionally(new TimeoutException("Request timeout"));
                it.remove();
            }
        }
    }

    private Optional<Session> getSession() {
        try {
            return Optional.ofNullable(future2.getNow(null));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Close the socket for the given reason.
     * @param reason of the closing
     */
    public void close(Throwable reason) {
        synchronized (SESSION_LOCK) {
            future2.completeExceptionally(reason);

            future1.thenAccept(s -> {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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
