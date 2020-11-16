package com.github.dedis.student20_pop.model.network;

import java.util.Objects;

public abstract class Request extends ChanneledMessage {

    private final transient int id;

    protected Request(String channel, int id) {
        super(channel);
        this.id = id;
    }

    public int getRequestID() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Request request = (Request) o;
        return id == request.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
