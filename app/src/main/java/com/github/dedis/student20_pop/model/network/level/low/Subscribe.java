package com.github.dedis.student20_pop.model.network.level.low;

public final class Subscribe extends Request {

    public Subscribe(String channel, int id) {
        super(channel, id);
    }

    @Override
    public String getMethod() {
        return Method.SUBSCRIBE.getMethod();
    }
}
