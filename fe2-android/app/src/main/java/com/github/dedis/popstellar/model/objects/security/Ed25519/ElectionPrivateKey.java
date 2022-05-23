package com.github.dedis.popstellar.model.objects.security.Ed25519;

import java.nio.charset.StandardCharsets;

import ch.epfl.dedis.lib.crypto.Ed25519Point;
import ch.epfl.dedis.lib.crypto.Ed25519Scalar;
import ch.epfl.dedis.lib.crypto.Point;
import ch.epfl.dedis.lib.crypto.Scalar;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import io.reactivex.annotations.NonNull;

public class ElectionPrivateKey {

    private static int MESSAGE_BYTE_SIZE = 64;
    private static int HALF_MESSAGE_BYTE_SIZE = 32;

    private final Ed25519Scalar scalar;

    public ElectionPrivateKey(String publicKey) {
        scalar = new Ed25519Scalar(publicKey);
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

    public Scalar getScalar() {
        return scalar;
    }

    public byte[] decrypt(@NonNull String message) throws CothorityCryptoException {

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
            e.printStackTrace();
            throw new CothorityCryptoException("Could not create K Point while decrypting");
        }

        Point S = K.mul(getScalar());
        // TODO: add final subtraction for both S and C
        return null;
    }


}
