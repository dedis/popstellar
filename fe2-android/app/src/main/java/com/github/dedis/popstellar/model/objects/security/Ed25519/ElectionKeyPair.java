package com.github.dedis.popstellar.model.objects.security.Ed25519;

import ch.epfl.dedis.lib.crypto.Ed25519;
import ch.epfl.dedis.lib.crypto.Ed25519Point;
import ch.epfl.dedis.lib.crypto.Point;
import ch.epfl.dedis.lib.crypto.Scalar;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import io.reactivex.annotations.NonNull;

public class ElectionKeyPair {

    private ElectionPrivateKey decryptionScheme;
    private ElectionPublicKey encryptionScheme;

    public ElectionKeyPair(@NonNull ElectionPublicKey encryptionScheme, @NonNull ElectionPrivateKey decryptionScheme) {
        this.decryptionScheme = decryptionScheme;
        this.encryptionScheme = encryptionScheme;
    }

    /**
     * Generate public / private keys for decryption / encryption
     *
     * @return ElectionKeyPair set (ElectionPublicKey=encryptionScheme,ElectionPrivateKey=encryptionScheme)
     * @throws CothorityCryptoException
     */
    public static ElectionKeyPair generateKeyPair() throws CothorityCryptoException {

        Scalar a = Ed25519.prime_order;
        Point aP = Ed25519Point.base().mul(a);

        ElectionPublicKey draftEncScheme =
                new ElectionPublicKey(aP.toBytes().toString());
        ElectionPrivateKey draftDecScheme =
                new ElectionPrivateKey(a.toBytes().toString());

        return new ElectionKeyPair(draftEncScheme, draftDecScheme);

    }

}
