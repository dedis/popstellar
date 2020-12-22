package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.low.Catchup;
import com.github.dedis.student20_pop.model.network.level.low.Query;
import com.github.dedis.student20_pop.model.network.level.low.Message;
import com.github.dedis.student20_pop.model.network.level.low.Broadcast;
import com.github.dedis.student20_pop.model.network.level.low.Publish;
import com.github.dedis.student20_pop.model.network.level.low.Subscribe;
import com.github.dedis.student20_pop.model.network.level.low.Unsubscribe;
import com.github.dedis.student20_pop.model.network.level.low.answer.Answer;
import com.github.dedis.student20_pop.model.network.level.low.answer.Error;
import com.github.dedis.student20_pop.model.network.level.low.answer.Result;
import com.github.dedis.student20_pop.model.network.level.mid.MessageGeneral;
import com.github.dedis.student20_pop.utility.json.JsonUtils;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the publish-subscribe protocol
 */
public final class LowLevelClientProxy {

    public static final long TIMEOUT = 5000L; // 5 secs

    private final Session session;

    private final Gson gson = JsonUtils.createGson();
    private final Map<Integer, RequestEntry> requests = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    public LowLevelClientProxy(Session session) {
        this.session = session;
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
        session.getAsyncRemote().sendText(txt);

        return entry.requests.thenApply(elem -> gson.fromJson(elem, responseType));
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
    public CompletableFuture<Integer> publish(String sender, String key, String channel, Data message) {
        String data = Base64.getEncoder().encodeToString(gson.toJson(message, Data.class).getBytes(StandardCharsets.UTF_8));
        String signature = Signature.sign(key, data);
        String msgId = Hash.hash(data, signature);
        MessageGeneral container = new MessageGeneral(sender, data, signature, msgId, new ArrayList<>());
        return makeRequest(Integer.class, id -> new Publish(channel, id, container));
    }

    /**
     * Catchup on missed messages from a given channel
     *
     * @param channel to fetch from
     */
    public CompletableFuture<List<Data>> catchup(String channel) {
        CompletableFuture<Data[]> future = makeRequest(Data[].class, id -> new Catchup(channel, id));
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
            handleMessage(gson.fromJson(obj, Broadcast.class));
        } else {
            handleResult(gson.fromJson(obj, Answer.class));
        }
    }

    /**
     * Handles a received Answer. Find tis matching request and complete it.
     *
     * @param answer received
     */
    private void handleResult(Answer answer) {
        RequestEntry entry = requests.remove(answer.getId());
        if (entry == null)
            throw new java.lang.Error("Received unknown Answer id");


        // There is a way this is the wrong Answer : if there was a timeout and the message came back while another was generated on the same id.
        // But this is a very rare case and we could add a timestamp to the protocol to fix this issue
        if (answer instanceof Result) {
            Result result = (Result) answer;
            entry.requests.complete(result.getResult());
        } else if (answer instanceof Error) {
            Error error = (Error) answer;
            entry.requests.completeExceptionally(new RuntimeException("Error code " + error.getError().getCode() + " : " + error.getError().getDescription()));
        } else {
            throw new java.lang.Error("Unknown Answer class");
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

    /**
     * @return the web socket session
     */
    public Session getSession() {
        return session;
    }

    //TODO Call this periodically
    public void purge() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, RequestEntry>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            RequestEntry entry = it.next().getValue();
            if (currentTime - entry.timestamp > TIMEOUT) {
                entry.requests.completeExceptionally(new TimeoutException("Query timeout"));
                it.remove();
            }
        }
    }

    private final static class RequestEntry {

        private final long timestamp = System.currentTimeMillis();
        private final CompletableFuture<JsonElement> requests = new CompletableFuture<>();
    }
}
