package com.github.dedis.student20_pop.model.network.level.mid;

import java.util.List;
import java.util.Objects;

/**
 * Container of a high level message.
 * <p>
 * It is encapsulated inside low level messages
 */
public class MessageGeneral {

    private final String sender;
    private final String data;
    private final String signature;
    private final String message_id;
    private final List<String> witness_signatures;

    public MessageGeneral(String sender, String data, String signature, String message_id, List<String> witness_signatures) {
        this.sender = sender;
        this.data = data;
        this.signature = signature;
        this.message_id = message_id;
        this.witness_signatures = witness_signatures;
    }

    public String getSender() {
        return sender;
    }

    public String getData() {
        return data;
    }

    public String getSignature() {
        return signature;
    }

    public String getMessage_id() {
        return message_id;
    }

    public List<String> getWitness_signatures() {
        return witness_signatures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageGeneral that = (MessageGeneral) o;
        return Objects.equals(getSender(), that.getSender()) &&
                Objects.equals(getData(), that.getData()) &&
                Objects.equals(getSignature(), that.getSignature()) &&
                Objects.equals(getMessage_id(), that.getMessage_id()) &&
                Objects.equals(getWitness_signatures(), that.getWitness_signatures());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSender(), getData(), getSignature(), getMessage_id(), getWitness_signatures());
    }
}
