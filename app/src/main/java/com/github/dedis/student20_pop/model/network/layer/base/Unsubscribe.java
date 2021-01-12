package com.github.dedis.student20_pop.model.network.layer.base;

/**
 * Unsubscribe from a channel
 */
public final class Unsubscribe extends Query {

    public Unsubscribe(String channel, int id) {
        super(channel, id);
    }

    @Override
    public String getMethod() {
        return Method.UNSUBSCRIBE.getMethod();
    }
}
