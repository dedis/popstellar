package com.github.dedis.student20_pop.utility.protocol;

import com.github.dedis.student20_pop.model.network.level.high.Data;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LowLevelProxy extends Closeable {

    /**
     * Minimum number of millisecond a request can stay unanswered before it is classified as timeout
     */
    long REQUEST_TIMEOUT = 5000; // 5s

    /**
     * Subscribe to a channel
     *
     * @param channel to subscribe to
     * @return a completable future holding the response value
     */
    CompletableFuture<Integer> subscribe(String channel);

    /**
     * Unsubscribe to a channel
     *
     * @param channel to subscribe to
     * @return a completable future holding the response value
     */
    CompletableFuture<Integer> unsubscribe(String channel);

    /**
     * Publish an event on given channel
     *
     * @param sender  of the data
     * @param key     public key of the sender
     * @param channel to publish the event on
     * @param message to publish
     * @return a completable future holding the response value
     */
    CompletableFuture<Integer> publish(String sender, String key, String channel, Data message);

    /**
     * Catchup on missed messages from a given channel
     *
     * @param channel to fetch from
     */
    CompletableFuture<List<Data>> catchup(String channel);

    /**
     * Lookup the request table and remove the ones that have timed out.
     */
    void purgeTimeoutRequests();

    /**
     * Close the socket for the given reason.
     *
     * @param reason of the closing
     */
    void close(Throwable reason);
}
