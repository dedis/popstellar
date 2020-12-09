package com.github.dedis.student20_pop.utility.network;

import android.util.Base64;

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the publish-subscribe protocol
 */
public final class LowLevelClientProxy {

    private static final long TIMEOUT = 5000L; // 5 secs

    private final Session session;

    private final Gson gson = JsonUtils.createGson();
    private final Map<Integer, RequestEntry> requests = new ConcurrentHashMap<>();
    private SplittableRandom rand = new SplittableRandom();

    public LowLevelClientProxy(Session session) {
        this.session = session;
    }

    /**
     * Make a request to the connected session.
     * Generate a unique id and save the the CompletableFuture that will be complete once a response is received.
     *
     * @param responseType the expected type of the response data
     * @param requestSupplier a generator that take as input the id of the request and output the actual request object
     * @param <T> generic type of the expected response data
     *
     * @return a CompletableFuture that will be completed when the response is received (or if it timeouts)
     */
    private <T> CompletableFuture<T> makeRequest(Class<T> responseType, Function<Integer, Request> requestSupplier) {
        RequestEntry entry = new RequestEntry();
        // Put the result to a random id, if it is already taken, generate a new id until it fits
        int id;
        do {
            id = rand.nextInt();
        }while(requests.putIfAbsent(id, entry) != null);

        Request request = requestSupplier.apply(id);
        String txt = gson.toJson(request, ChanneledMessage.class);
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
     *
     * @return a completable future holding the response value
     */
    public CompletableFuture<Integer> publish(String sender, String key, String channel, Message message) {
        String data = Base64.encodeToString(gson.toJson(message, Message.class).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
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
     * @param msg received
     */
    void onMessage(String msg) {
        JsonObject obj = gson.fromJson(msg, JsonObject.class);
        if(obj.has("method")) {
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
        if(entry == null)
            throw new Error("Received unknown result id");


        // There is a way this is the wrong answer : if there was a timeout and the message came back while another was generated on the same id.
        // But this is a very rare case and we could add a timestamp to the protocol to fix this issue
        if(result instanceof Success) {
            Success success = (Success) result;
            entry.requests.complete(success.getResult());
        } else if(result instanceof Failure) {
            Failure failure = (Failure) result;
            entry.requests.completeExceptionally(new RuntimeException("Error code " + failure.getError().getCode() + " : " + failure.getError().getDescription()));
        } else {
            throw new Error("Unknown result class");
        }
    }

    /**
     * Extract the high level message from the low level received message and handles it
     *
     * TODO Handle the messages
     * @param lowLevelMessage the low level message received
     */
    private void handleMessage(LowLevelMessage lowLevelMessage) {
        MessageContainer container = lowLevelMessage.getMessage();
        Message message = gson.fromJson(
                new String(
                        Base64.decode(container.getData(), Base64.DEFAULT),
                        StandardCharsets.UTF_8),
                Message.class);
        System.out.println(message);
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
            if(currentTime - entry.timestamp > TIMEOUT) {
                entry.requests.completeExceptionally(new TimeoutException("Request timeout"));
                it.remove();
            }
        }
    }

    private final static class RequestEntry {

        private final long timestamp = System.currentTimeMillis();
        private final CompletableFuture<JsonElement> requests = new CompletableFuture<>();
    }
}
