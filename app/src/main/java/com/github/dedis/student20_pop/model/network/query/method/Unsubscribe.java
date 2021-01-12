package com.github.dedis.student20_pop.model.network.query.method;

import com.github.dedis.student20_pop.model.network.query.Method;
import com.github.dedis.student20_pop.model.network.query.Query;

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
