package com.github.dedis.student20_pop.model.network.level.low;

/**
 * Unsubscribe from a channel
 */
public final class Unsubscribe extends Request {

    public Unsubscribe(String channel, int id) {
        super(channel, id);
    }

    @Override
    public String getMethod() {
        return Method.UNSUBSCRIBE.getMethod();
    }
}
