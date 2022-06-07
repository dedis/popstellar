package com.github.dedis.popstellar.model.objects.security.elGamal;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import ch.epfl.dedis.lib.crypto.Ed25519Pair;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import io.reactivex.annotations.NonNull;

public class ElectionKeyPair {

    private final ElectionPrivateKey decryptionScheme;
    private final ElectionPublicKey encryptionScheme;

    public ElectionKeyPair(@NonNull ElectionPublicKey encryptionScheme, @NonNull ElectionPrivateKey decryptionScheme) {
        this.decryptionScheme = decryptionScheme;
        this.encryptionScheme = encryptionScheme;
    }

    //Set basic getters useful for encrypt/decrypt
    public ElectionPrivateKey getDecryptionScheme() {
        return decryptionScheme;
    }

    public ElectionPublicKey getEncryptionScheme() {
        return encryptionScheme;
    }

    /**
     * Generate public / private keys for decryption / encryption
     *
     * @return ElectionKeyPair set (ElectionPublicKey=encryptionScheme,ElectionPrivateKey=encryptionScheme)
     * @throws CothorityCryptoException
     */
    public static ElectionKeyPair generateKeyPair() {
        // Generate at random using Ed25519Pair class
        Ed25519Pair keyPairScheme = new Ed25519Pair();
        Base64URLData encodedPublic = new Base64URLData(keyPairScheme.point.toBytes());
        Base64URLData privateKeyPrivate = new Base64URLData(keyPairScheme.scalar.toBytes());

        ElectionPublicKey draftEncScheme =
                new ElectionPublicKey(encodedPublic);
        ElectionPrivateKey draftDecScheme =
                new ElectionPrivateKey(privateKeyPrivate);

        return new ElectionKeyPair(draftEncScheme, draftDecScheme);
    }

}
