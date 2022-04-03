package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Address;

public class AddDummyTransaction extends Data {
    //Test Transaction don't really look like the json Schema
    @Nullable
    private final Address receiver_address;
    private final Address sender_address;
    private final int amount;

    public AddDummyTransaction(int amount, @Nullable Address sender_address, @Nullable Address receiver_address){
        this.sender_address = sender_address;
        this.receiver_address = receiver_address;

        //TODO: This Amount has to be changed
        // Limit of the sender account
        this.amount = amount;
    }

    @Nullable
    public Address getReceiver_address() {
        return receiver_address;
    }

    public Address getSender_address() {
        return sender_address;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddDummyTransaction that = (AddDummyTransaction) o;
        return amount == that.amount && java.util.Objects.equals(receiver_address, that.receiver_address) && java.util.Objects.equals(sender_address, that.sender_address);
    }

    @Override
    public String toString() {
        return "AddTransaction{" +
                "receiver_address=" + receiver_address +  '\'' +
                ", sender_address=" + sender_address + '\'' +
                ", amount=" + amount + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(receiver_address, sender_address, amount);
    }

    @Override
    public String getObject() {
        return Objects.TRANSACTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.ADD.getAction();
    }

}
