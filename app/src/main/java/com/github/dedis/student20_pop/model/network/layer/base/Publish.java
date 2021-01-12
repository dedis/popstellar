package com.github.dedis.student20_pop.model.network.layer.base;

import com.github.dedis.student20_pop.model.network.layer.message.MessageGeneral;

import java.util.Objects;

/**
 * Publish a message on a channel
 */
public final class Publish extends Query {

    private final MessageGeneral message;

    public Publish(String channel, int id, MessageGeneral message) {
        super(channel, id);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.PUBLISH.getMethod();
    }

    public MessageGeneral getMessage() {
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
