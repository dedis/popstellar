package com.github.dedis.student20_pop.model.network.layer.base;

import java.util.Objects;

/**
 * An abstract low level message that is sent over a specific channel
 */
public abstract class Message {

    private final String channel;

    /**
     * Constructor for a Message
     *
     * @param channel the channel over which the message is sent
     */
    protected Message(String channel) {
        this.channel = channel;
    }

    /**
     * Return the Message method
     */
    public abstract String getMethod();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }
}
