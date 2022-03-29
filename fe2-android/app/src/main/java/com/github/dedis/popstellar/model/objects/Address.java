package com.github.dedis.popstellar.model.objects;

public class Address {

    //Represent the address to which if a send and receiver of a transaction
    // is actually the public key of some user I think
    //String sender_address;
    //String receiver_address;
    private final String address;

    public Address(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
