package com.github.dedis.student20_pop.model;

import com.google.crypto.tink.subtle.Ed25519Sign;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;

/**
 * Class modeling a set of Public and Private Keys
 */
public final class Keys {

    private String publicKey;
    private String privateKey;

    /**
     * Constructor for the Keys
     *
     * Generates a set of public and private keys using Ed25519.
     */
    public Keys() {
        // Using the tink.subtle package for the moment, not sure we can keep using it
        //("While they're generally safe to use, they're not meant for public consumption and
        // can be modified in any way, or even removed, at any time.")
        try {
            Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
            this.publicKey = Base64.getEncoder().encodeToString(keyPair.getPublicKey());
            this.privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivateKey());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the public key encoded in Base64.
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Returns the private key encoded in Base64.
     */
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