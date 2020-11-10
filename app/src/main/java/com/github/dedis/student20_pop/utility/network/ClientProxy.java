package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Event;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the publish-subscribe protocol
 */
public final class ClientProxy {

    private final Session session;

    public ClientProxy(Session session) {
        this.session = session;
    }

    /**
     * Subscribe to a channel
     *
     * @param channel to subscribe to
     */
    public void subscribe(String channel) {
        //TODO
    }

    /**
     * Publish an event on given channel
     *
     * @param channel to publish the event on
     * @param event to publish
     */
    public void publish(String channel, Event event) {
        //TODO
    }

    /**
     * fetch events from the given channel
     *
     * @param channel to fetch from
     * @param id of the events (Type might change)
     */
    public void fetch(String channel, String id) {
        //TODO fetch data. Is it a request ?
    }

    /**
     * Called by the underlying socket endpoint when a message is received
     * @param msg received
     */
    public void onMessage(String msg) {
        //TODO convert msg to object with json
    }

    // TODO remove this once not needed for testing (when the tests uses the publish-subscribe protocol
    @Deprecated
    public Session getSession() {
        return session;
    }
}
