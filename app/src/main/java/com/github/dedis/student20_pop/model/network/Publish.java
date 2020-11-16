package com.github.dedis.student20_pop.model.network;

import com.github.dedis.student20_pop.model.network.message.Message;

import java.util.Objects;

public final class Publish extends Request {

    private final Message message;

    public Publish(String channel, int id, Message message) {
        super(channel, id);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.PUBLISH.getMethod();
    }

    public Message getMessage() {
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
