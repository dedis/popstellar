package com.github.dedis.student20_pop.model.network.method;

import com.github.dedis.student20_pop.utility.protocol.MessageHandler;

/**
 * Unsubscribe from a channel
 */
public final class Unsubscribe extends Query {

    public Unsubscribe(String channel, int id) {
        super(channel, id);
    }

    @Override
    public void accept(MessageHandler handler) {
        handler.handle(this);
    }

    @Override
    public String getMethod() {
        return Method.UNSUBSCRIBE.getMethod();
    }
}
