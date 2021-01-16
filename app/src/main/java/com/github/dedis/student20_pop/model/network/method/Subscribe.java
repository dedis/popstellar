package com.github.dedis.student20_pop.model.network.method;

import com.github.dedis.student20_pop.utility.protocol.MessageHandler;

/**
 * Subscribe to a channel
 */
public final class Subscribe extends Query {

    public Subscribe(String channel, int id) {
        super(channel, id);
    }

    @Override
    public void accept(MessageHandler handler) {
        handler.handle(this);
    }

    @Override
    public String getMethod() {
        return Method.SUBSCRIBE.getMethod();
    }
}
