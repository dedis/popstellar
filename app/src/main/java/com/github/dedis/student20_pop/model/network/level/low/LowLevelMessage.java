package com.github.dedis.student20_pop.model.network.level.low;

import com.github.dedis.student20_pop.model.network.level.mid.MessageContainer;

import java.util.Objects;

public final class LowLevelMessage extends ChanneledMessage {

    private final MessageContainer message;

    public LowLevelMessage(String channel, MessageContainer message) {
        super(channel);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.MESSAGE.getMethod();
    }

    public MessageContainer getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LowLevelMessage that = (LowLevelMessage) o;
        return Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMessage());
    }
}
