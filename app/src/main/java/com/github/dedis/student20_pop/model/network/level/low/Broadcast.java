package com.github.dedis.student20_pop.model.network.level.low;

import com.github.dedis.student20_pop.model.network.level.mid.MessageGeneral;

import java.util.Objects;

/**
 * Broadcast a high level message inside a container.
 * <p>
 * Does not expect any answer
 */
public final class Broadcast extends Message {

    private final MessageGeneral message;

    public Broadcast(String channel, MessageGeneral message) {
        super(channel);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.MESSAGE.getMethod();
    }

    public MessageGeneral getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Broadcast that = (Broadcast) o;
        return Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMessage());
    }
}
