package com.github.dedis.student20_pop.model;

import java.util.Objects;

/**
 * Class modeling a set of Public and Private Keys
 */
public class Keys {

    private String publicKey;
    private String privateKey;

    /**
     * Empty constructor for the Keys
     */
    public Keys() {
        // empty for now
    }

    /**
     * Generate a set of public and private keys
     */
    public void generateKeys() {
        // simple for now
        this.publicKey = "";
        this.privateKey = "";
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keys keys = (Keys) o;
        return Objects.equals(publicKey, keys.publicKey) &&
                Objects.equals(privateKey, keys.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey, privateKey);
    }
}
