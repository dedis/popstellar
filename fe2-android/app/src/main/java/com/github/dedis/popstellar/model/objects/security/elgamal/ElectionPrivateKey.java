package com.github.dedis.popstellar.model.objects.security.elgamal;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import java.util.Objects;

import ch.epfl.dedis.lib.crypto.Ed25519Point;
import ch.epfl.dedis.lib.crypto.Ed25519Scalar;
import ch.epfl.dedis.lib.crypto.Point;
import ch.epfl.dedis.lib.crypto.Scalar;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import io.reactivex.annotations.NonNull;

public class ElectionPrivateKey {

    private static final int MESSAGE_BYTE_SIZE = 64;
    private static final int HALF_MESSAGE_BYTE_SIZE = 32;

    // Scalar generated with the private key
    @NonNull
    private final Ed25519Scalar privateKey;

    /**
     * Create an decryption scheme for 64 bytes message using a private key
     *
     * @param privateKey private key used to decrypt
     */
    public ElectionPrivateKey(@NonNull Base64URLData privateKey) {
        this.privateKey = new Ed25519Scalar(privateKey.getData());
    }

    @Override
    public String toString() {
        return privateKey.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElectionPrivateKey that = (ElectionPrivateKey) o;
        return that.getPrivateKey().equals(getPrivateKey());
    }

    /**
     * Added this hash behavior by default
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPrivateKey().toString());
    }

    @NonNull
    public Scalar getPrivateKey() {
        return privateKey;
    }

    /**
     * Decypt the given string using ElGamal
     *
     * @param message message encrypted in Base64 to decrypt (64 byte length)
     * @return byte array containing the decrypted message
     * @throws CothorityCryptoException if problem while transforming final data into a byte array
     */
    public byte[] decrypt(@NonNull String message) throws CothorityCryptoException {

        Base64URLData decoded = new Base64URLData(message);
    // Follows this implementation:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L27-L31
    byte[] byteMessage = decoded.getData();
    if (byteMessage.length != MESSAGE_BYTE_SIZE) {
            throw new IllegalArgumentException("Your message to decrypt should contain exactly 64 bytes");
        }

        // Decompose into two bytes array for recuperating both C and K
        byte[] Kbytes = new byte[HALF_MESSAGE_BYTE_SIZE];
        byte[] Cbytes = new byte[HALF_MESSAGE_BYTE_SIZE];
        for (int i = 0; i < MESSAGE_BYTE_SIZE; i++) {
            if (i < HALF_MESSAGE_BYTE_SIZE) {
        Kbytes[i] = byteMessage[i];
            } else {
        Cbytes[i - HALF_MESSAGE_BYTE_SIZE] = byteMessage[i];
            }
        }
        Ed25519Point K;
        Ed25519Point C;
        try {
            K = new Ed25519Point(Kbytes);
            C = new Ed25519Point(Cbytes);
        } catch (CothorityCryptoException e) {
            throw new IllegalArgumentException("Could not create K Point while decrypting");
        }

        // Export data to get the original message
        Point S = K.mul(getPrivateKey());
        return S.add(C.negate()).data();
    }


}
