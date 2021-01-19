package com.github.dedis.student20_pop.utility.protocol;

import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.utility.network.WebSocketHighLevelProxy;
import com.github.dedis.student20_pop.utility.network.WebSocketLowLevelProxy;

import java.net.URI;

/**
 * Factory to create protocol proxies
 */
public final class ProtocolProxyFactory {

    private static final ProtocolProxyFactory INSTANCE = new ProtocolProxyFactory();

    private ProtocolProxyFactory() {}

    /**
     * Get the instance of the factory
     *
     * @return factory's instance
     */
    public static ProtocolProxyFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Create a fresh HighLevel Proxy given its host and the user of the application
     *
     * @param host of the backend
     * @param user of the app
     * @return the new proxy
     */
    public HighLevelProxy createHighLevelProxy(URI host, Person user, DataHandler dataHandler) {
        return new WebSocketHighLevelProxy(user, createLowLevelProxy(host, dataHandler));
    }

    /**
     * Create a fresh LowLevel Proxy given its host
     *
     * @param host of the backend
     * @return the new proxy
     */
    public LowLevelProxy createLowLevelProxy(URI host, DataHandler dataHandler) {
        return new WebSocketLowLevelProxy(host, dataHandler);
    }
}
