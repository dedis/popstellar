package com.github.dedis.student20_pop.model.network;

public final class Catchup extends Request {

    public Catchup(String channel, int id) {
        super(channel, id);
    }

    @Override
    public String getMethod() {
        return Method.CATCHUP.getMethod();
    }
}
