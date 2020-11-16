package com.github.dedis.student20_pop.model.network;

import java.util.Objects;

public abstract class ChanneledMessage {

    private final String channel;

    protected ChanneledMessage(String channel) {
        this.channel = channel;
    }

    public abstract String getMethod();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChanneledMessage that = (ChanneledMessage) o;
        return Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }
}
