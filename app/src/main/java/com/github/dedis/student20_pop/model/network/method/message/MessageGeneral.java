package com.github.dedis.student20_pop.model.network.method.message;

import java.util.List;
import java.util.Objects;

/**
 * Container of a high level message.
 * <p>
 * It is encapsulated inside low level messages
 */
public final class MessageGeneral {

    private final String sender;
    private final String data;
    private final String signature;
    private final String messageId;
    private final List<String> witnessSignatures;

    /**
     * Constructor for a MessageGeneral
     * @param sender public key of the sender/organizer/server
     * @param data data contained in the message
     * @param signature organizer's signature on data
     * @param messageId ID of the message
     * @param witnessSignatures signatures of the witnesses on the modification message (either creation/update)
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public MessageGeneral(String sender, String data, String signature, String messageId, List<String> witnessSignatures) {
        if(sender == null || data == null || signature == null || messageId == null ||
                witnessSignatures == null || witnessSignatures.contains(null)) {
            throw new IllegalArgumentException("Trying to create a general message with null parameters");
        }
        this.sender = sender;
        this.data = data;
        this.signature = signature;
        this.messageId = messageId;
        this.witnessSignatures = witnessSignatures;
    }

    /**
     * Returns public key of the sender.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Returns the data contained in the message.
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the organizer's signature on data.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns the message ID.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the signatures of the witnesses on the modification message (either creation/update).
     */
    public List<String> getWitnessSignatures() {
        return witnessSignatures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageGeneral that = (MessageGeneral) o;
        return Objects.equals(getSender(), that.getSender()) &&
                Objects.equals(getData(), that.getData()) &&
                Objects.equals(getSignature(), that.getSignature()) &&
                Objects.equals(getMessageId(), that.getMessageId()) &&
                Objects.equals(getWitnessSignatures(), that.getWitnessSignatures());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSender(), getData(), getSignature(), getMessageId(), getWitnessSignatures());
    }
}
