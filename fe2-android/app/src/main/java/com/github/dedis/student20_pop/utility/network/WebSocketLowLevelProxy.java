package com.github.dedis.student20_pop.utility.network;

import android.util.Log;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Error;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Catchup;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Query;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.Unsubscribe;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.utility.json.JsonUtils;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;
import com.github.dedis.student20_pop.utility.protocol.LowLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.MessageHandler;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

/** A proxy of a connection to a WebSocket. It encapsulate the publish-subscribe protocol */
public final class WebSocketLowLevelProxy implements LowLevelProxy, MessageListener {

  private static final String TAG = WebSocketLowLevelProxy.class.getName();

  // Lock to prevent multiple threads to access the session
  private final Object SESSION_LOCK = new Object();

  private final MessageHandler messageHandler = new WebSocketMessageHandler();
  private final Gson gson = JsonUtils.createGson();
  private final Map<Integer, RequestEntry> requests = new ConcurrentHashMap<>();
  private final AtomicInteger counter = new AtomicInteger();
  private final URI sessionURI;
  private final DataHandler dataHandler;
  // Having to different futures to be able to close the session even if it is closed during the
  // opening.
  // The first future holds the session and it will immediately be passed to the second on
  // completion
  private CompletableFuture<Session> session;
  // The second future is used to make requests.
  // If the connection is cancelled, this future complete exceptionally but future1 is kept clean
  // to be able to close the session as soon as it is created.
  private CompletableFuture<Session> sessionUse;

  public WebSocketLowLevelProxy(URI host, DataHandler dataHandler) {
    this.sessionURI = host;
    this.dataHandler = dataHandler;
    this.session = WebSocketEndpoint.connect(host, this);
    this.sessionUse = session.thenApply(s -> s);
  }

  /**
   * Make a request to the connected session. Generate a unique id and save the the
   * CompletableFuture that will be complete once a response is received.
   *
   * @param responseType the expected type of the response data
   * @param requestSupplier a generator that take as input the id of the request and output the
   *     actual request object
   * @param <T> generic type of the expected response data
   * @return a CompletableFuture that will be completed when the response is received (or if it
   *     timeouts)
   */
  private <T> CompletableFuture<T> makeRequest(
      Class<T> responseType, Function<Integer, Query> requestSupplier) {
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

    sessionUse
        .thenAccept(
            session -> {
              synchronized (SESSION_LOCK) {
                session.getAsyncRemote().sendText(txt);
              }
            })
        .exceptionally(
            t -> {
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
          // There was an error during competition, retry
          Log.d(
              TAG,
              "Connection to "
                  + sessionURI
                  + " was either lost or never made. Trying to reconnect...");
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
  public CompletableFuture<Integer> publish(
      String sender, String key, String channel, Data message) {
    String data =
        Base64.getEncoder()
            .encodeToString(gson.toJson(message, Data.class).getBytes(StandardCharsets.UTF_8));
    String signature = Signature.sign(key, data);
    String msgId = Hash.hash(data, signature);
    //    MessageGeneral container =
    //        new MessageGeneral(sender, data, signature, msgId, new ArrayList<>());
    //    return makeRequest(Integer.class, id -> new Publish(channel, id, container));
    return makeRequest(Integer.class, id -> new Publish(channel, id, null));
  }

  @Override
  public CompletableFuture<List<Data>> catchup(String channel) {
    CompletableFuture<Data[]> future = makeRequest(Data[].class, id -> new Catchup(channel, id));
    return future.thenApply(Arrays::asList);
  }

  @Override
  public void onMessage(String msg) {
    GenericMessage genericMessage = gson.fromJson(msg, GenericMessage.class);
    genericMessage.accept(messageHandler);
  }

  @Override
  public void purgeTimeoutRequests() {
    long currentTime = System.currentTimeMillis();

    Iterator<Map.Entry<Integer, RequestEntry>> requestsIterator = requests.entrySet().iterator();
    while (requestsIterator.hasNext()) {
      RequestEntry entry = requestsIterator.next().getValue();
      if (currentTime - entry.timestamp > LowLevelProxy.REQUEST_TIMEOUT) {
        entry.requests.completeExceptionally(new TimeoutException("Query timeout"));
        requestsIterator.remove();
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

      session.thenAccept(
          s -> {
            try {
              s.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    }

    // Complete all pending requests and remove them
    Iterator<Map.Entry<Integer, RequestEntry>> requestsIterator = requests.entrySet().iterator();
    while (requestsIterator.hasNext()) {
      requestsIterator.next().getValue().requests.completeExceptionally(reason);
      requestsIterator.remove();
    }
  }

  @Override
  public void close() {
    close(new IOException("Session closed"));
  }

  private static final class RequestEntry {

    private final long timestamp = System.currentTimeMillis();
    private final CompletableFuture<JsonElement> requests = new CompletableFuture<>();
  }

  private class WebSocketMessageHandler implements MessageHandler {

    @Override
    public void handle(Result result) {
      RequestEntry entry = requests.remove(result.getId());
      if (entry != null) entry.requests.complete(result.getResult());
      else Log.d(TAG, "Received an answer with unknown id. Did it timeout ?");
    }

    @Override
    public void handle(Error error) {
      RequestEntry entry = requests.remove(error.getId());
      if (entry != null)
        entry.requests.completeExceptionally(
            new RuntimeException(
                "Error code "
                    + error.getError().getCode()
                    + " : "
                    + error.getError().getDescription()));
      else Log.d(TAG, "Received an answer with unknown id. Did it timeout ?");
    }

    @Override
    public void handle(Broadcast broadcast) {
      MessageGeneral container = broadcast.getMessage();
      //      Data data =
      //          gson.fromJson(
      //              new String(Base64.getDecoder().decode(container.getData()),
      // StandardCharsets.UTF_8),
      ////              Data.class);
      //
      //      data.accept(dataHandler, sessionURI, broadcast.getChannel());
    }
  }
}
