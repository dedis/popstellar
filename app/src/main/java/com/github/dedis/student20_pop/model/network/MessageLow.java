package com.github.dedis.student20_pop.model.network;

import com.github.dedis.student20_pop.model.network.message.Message;

import java.util.Objects;

public final class MessageLow extends ChanneledMessage {

    private final Message message;

    public MessageLow(String channel, Message message) {
        super(channel);
        this.message = message;
    }

    @Override
    public String getMethod() {
        return Method.MESSAGE.getMethod();
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MessageLow that = (MessageLow) o;
        return Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMessage());
    }
}
