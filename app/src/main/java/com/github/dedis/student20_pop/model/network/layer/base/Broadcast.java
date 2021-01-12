package com.github.dedis.student20_pop.model.network.layer.base;

import com.github.dedis.student20_pop.model.network.layer.message.MessageGeneral;

import java.util.Objects;

/**
 * Broadcast a high level message inside a container.
 * <p>
 * Does not expect any answer
 */
public final class Broadcast extends Message {

    private final MessageGeneral message;

    /**
     * Constructor for a Broadcast
     *
     * @param channel name of the channel
     * @param message the message to broadcast
     * @throws IllegalArgumentException if any parameter is null
     */
    public Broadcast(String channel, MessageGeneral message) {
        super(channel);
        if(message == null) {
            throw new IllegalArgumentException("Trying to broadcast a null message");
        }
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.MESSAGE.getMethod();
    }

    /**
     * Returns the message of the Broadcast.
     */
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
