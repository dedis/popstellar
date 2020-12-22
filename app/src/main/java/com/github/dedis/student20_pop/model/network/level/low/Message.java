package com.github.dedis.student20_pop.model.network.level.low;

import java.util.Objects;

/**
 * An abstract low level message that is sent over a specific channel
 */
public abstract class Message {

    private final String channel;

    protected Message(String channel) {
        this.channel = channel;
    }

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
