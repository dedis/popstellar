package com.github.dedis.student20_pop.model.network.layer.base;

/**
 * Query to catchup on missed messages
 */
public final class Catchup extends Query {

    /**
     * Constructor for a Catchup
     *
     * @param channel name of the channel
     * @param id request ID of the catchup
     */
    public Catchup(String channel, int id) {
        super(channel, id);
    }

    @Override
    public String getMethod() {
        return Method.CATCHUP.getMethod();
    }
}
