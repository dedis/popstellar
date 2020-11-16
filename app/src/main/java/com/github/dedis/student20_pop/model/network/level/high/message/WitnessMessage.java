package com.github.dedis.student20_pop.model.network.level.high.message;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

public class WitnessMessage extends Message {

    private final String message_id;
    private final String signature;

    public WitnessMessage(String message_id, String signature) {
        this.message_id = message_id;
        this.signature = signature;
    }

    @Override
    public String getObject() {
        return Objects.MESSAGE.getObject();
    }

    @Override
    public String getAction() {
        return Action.WITNESS.getAction();
    }

    public String getMessage_id() {
        return message_id;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WitnessMessage that = (WitnessMessage) o;
        return java.util.Objects.equals(getMessage_id(), that.getMessage_id()) &&
                java.util.Objects.equals(getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getMessage_id(), getSignature());
    }
}
