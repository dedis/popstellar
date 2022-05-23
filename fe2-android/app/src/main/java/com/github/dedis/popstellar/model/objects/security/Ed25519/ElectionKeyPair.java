package com.github.dedis.popstellar.model.objects.security.Ed25519;

import ch.epfl.dedis.lib.crypto.Ed25519Pair;
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

        Ed25519Pair keyPairScheme = new Ed25519Pair();

        ElectionPublicKey draftEncScheme =
                new ElectionPublicKey(keyPairScheme.point.toBytes().toString());
        ElectionPrivateKey draftDecScheme =
                new ElectionPrivateKey(keyPairScheme.scalar.toBytes().toString());

        return new ElectionKeyPair(draftEncScheme, draftDecScheme);

    }

}
