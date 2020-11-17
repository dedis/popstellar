package com.github.dedis.student20_pop.utility.network;

import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;

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
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the publish-subscribe protocol
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public final class LowLevelClientProxy {

    private final Session session;

    private final Gson gson = JsonUtils.createGson();
    private final Map<Integer, CompletableFuture<JsonElement>> requests = new ConcurrentHashMap<>();
    private SplittableRandom rand = new SplittableRandom();

    public LowLevelClientProxy(Session session) {
        this.session = session;
    }

    private <T> CompletableFuture<T> makeRequest(Class<T> requestType, Function<Integer, Request> requestSupplier) {
        CompletableFuture<JsonElement> result = new CompletableFuture<>();
        // Put the result to a random id, if it is already taken, generate a new id until it fits
        int id;
        do {
            id = rand.nextInt();
        }while(requests.putIfAbsent(id, result) != null);

        Request request = requestSupplier.apply(id);
        session.getAsyncRemote().sendText(gson.toJson(request, ChanneledMessage.class));

        return result.thenApply(elem -> gson.fromJson(elem, requestType));
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<Integer> publish(String sender, String channel, Message message) {
        String data = Base64.encodeToString(gson.toJson(message, Message.class).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        String signature = Signature.sign(/* TODO */ "sign", data);
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

    private void handleResult(Result result) {
        CompletableFuture<JsonElement> future = requests.remove(result.getId());
        if(future == null)
            throw new Error("Received unknown result id");

        // I love match cases
        if(result instanceof Success) {
            Success success = (Success) result;
            future.complete(success.getResult());
        } else if(result instanceof Failure) {
            Failure failure = (Failure) result;
            future.completeExceptionally(new RuntimeException("Error code " + failure.getError().getCode() + " : " + failure.getError().getDescription()));
        } else {
            throw new Error("Unknown result class");
        }
    }

    private void handleMessage(LowLevelMessage lowLevelMessage) {
        MessageContainer container = lowLevelMessage.getMessage();
        Message message = gson.fromJson(
                new String(
                        Base64.decode(container.getData(), Base64.DEFAULT),
                        StandardCharsets.UTF_8),
                Message.class);
        System.out.println(message);
    }

    // TODO remove this once not needed for testing (when the tests uses the publish-subscribe protocol
    @Deprecated
    public Session getSession() {
        return session;
    }
}
