package com.github.dedis.popstellar.model.objects.security.Ed25519;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import java.nio.charset.StandardCharsets;

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
    private final Ed25519Scalar scalar;

    /**
     * Create an decryption scheme for 64 bytes message using a private key
     *
     * @param privateKey private key used to decrypt
     */
    public ElectionPrivateKey(@NonNull Base64URLData privateKey) {
        scalar = new Ed25519Scalar(privateKey.getData());
    }

    @Override
    public String toString() {
        return scalar.toString();
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
        return that.getScalar().equals(getScalar());
    }

    /**
     * Added this hash behavior by default
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(getScalar().toString());
    }

    public Scalar getScalar() {
        return scalar;
    }

    /**
     * Decypt the given string using ElGamal
     *
     * @param message message to decrypt (64 byte length)
     * @return byte array containing the decrypted message
     * @throws CothorityCryptoException
     */
    public byte[] decrypt(@NonNull String message) throws CothorityCryptoException {

        // Follows this implementation:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L27-L31

        byte[] in_byte_message = message.getBytes(StandardCharsets.UTF_8);
        if (in_byte_message.length != MESSAGE_BYTE_SIZE) {
            throw new IllegalArgumentException("Your message to decrypt should contain exactly 64 bytes");
        }
        // Decompose into two bytes array for recuperating both C and K
        byte[] K_bytes = new byte[HALF_MESSAGE_BYTE_SIZE];
        byte[] C_bytes = new byte[HALF_MESSAGE_BYTE_SIZE];
        for (int i = 0; i < MESSAGE_BYTE_SIZE; i++) {
            if (i < HALF_MESSAGE_BYTE_SIZE) {
                K_bytes[i] = in_byte_message[i];
            } else {
                C_bytes[i] = in_byte_message[i];
            }
        }
        Ed25519Point K;
        Ed25519Point C;
        try {
            K = new Ed25519Point(K_bytes);
            C = new Ed25519Point(C_bytes);
        } catch (CothorityCryptoException e) {
            throw new CothorityCryptoException("Could not create K Point while decrypting");
        }

        // Substract and export data to get the original message
        Point S = K.mul(getScalar());
        return S.add(C.negate()).data();
    }


}
