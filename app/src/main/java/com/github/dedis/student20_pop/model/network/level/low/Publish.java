package com.github.dedis.student20_pop.model.network.level.low;

import com.github.dedis.student20_pop.model.network.level.mid.MessageContainer;

import java.util.Objects;

public final class Publish extends Request {

    private final MessageContainer message;

    public Publish(String channel, int id, MessageContainer message) {
        super(channel, id);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.PUBLISH.getMethod();
    }

    public MessageContainer getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Publish publish = (Publish) o;
        return Objects.equals(getMessage(), publish.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMessage());
    }
}
