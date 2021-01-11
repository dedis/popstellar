package com.github.dedis.student20_pop.utility.network;

/**
 * Listener of received messages by the websocket
 */
public interface IMessageListener {

    /**
     * Called by the socket endpoint when a message is received
     *
     * @param msg received
     */
    void onMessage(String msg);
}
