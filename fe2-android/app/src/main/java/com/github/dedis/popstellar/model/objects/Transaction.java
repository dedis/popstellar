package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

public class Transaction {
    private Channel channel;
    private PublicKey sender;
    /* some Txin Txout*/

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
}
