package com.github.dedis.student20_pop.model;

import java.security.GeneralSecurityException;
import java.util.Objects;
import com.google.crypto.tink.subtle.Ed25519Sign;
import com.google.crypto.tink.subtle.Hex;


/**
 * Class modeling a set of Public and Private Keys
 */
public final class Keys {

    private String publicKey;
    private String privateKey;

    /**
     * Constructor for the Keys, generates a set of public and private keys
     */
    public Keys() {
        // Using the tink.subtle package for the moment, not sure we can keep using it
        //("While they're generally safe to use, they're not meant for public consumption and can be modified in any way, or even removed, at any time.")
        try {
            Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
            this.publicKey = Hex.encode(keyPair.getPublicKey());
            this.privateKey = Hex.encode(keyPair.getPrivateKey()); //use Hex.decode to get byte[]
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    //Not sure we should have such a method, if the signature happens inside this class
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