package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

public class DummyCoin {

    private MessageID id;
    private Channel channel;

    private PublicKey sender;
    private Address receiver_address;
    private Address sender_address;
    private int amount;

    public DummyCoin(MessageID id) {
        if (id == null) {
            throw new IllegalArgumentException("The id is null");
        } else if (id.getEncoded().isEmpty()) {
            throw new IllegalArgumentException("The id of the Dummy Coin is empty");
        }
        this.id = id;
    }

    public MessageID getId() {
        return id;
    }

    public void setId(MessageID id) {
        if (id == null) {
            throw new IllegalArgumentException("The id is null");
        } else if (id.getEncoded().isEmpty()) {
            throw new IllegalArgumentException("The id of the Chirp is empty");
        }
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(@NonNull Channel channel) {
        this.channel = channel;
    }

    public PublicKey getSender() {
        return sender;
    }

    public void setSender(PublicKey sender) {
        this.sender = sender;
    }

    public Address getReceiver_address() {
        return receiver_address;
    }

    public void setReceiver_address(Address receiver_address) {
        this.receiver_address = receiver_address;
    }

    public Address getSender_address() {
        return sender_address;
    }

    public void setSender_address(Address sender_address) {
        this.sender_address = sender_address;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}

