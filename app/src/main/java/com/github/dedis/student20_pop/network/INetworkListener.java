package com.github.dedis.student20_pop.network;

/**
 * A listener that receive network data
 *
 * Must be registered to a WebSocket
 */
public interface INetworkListener {

    /**
     * Called by a WebSocket when it receives data
     * @param data received
     */
    void receive(String data);
}
