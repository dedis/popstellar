package com.github.dedis.popstellar.model.objects.security.ed25519;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPrivateKey;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import ch.epfl.dedis.lib.crypto.Ed25519Pair;

public class ElectionPrivateKeyTest {


    private final String nonValidMockElectionKeyString = "uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ=";
    private final Base64URLData nonValidMockEncodedElectionKey = new Base64URLData(nonValidMockElectionKeyString.getBytes(StandardCharsets.UTF_8));

    private final Ed25519Pair keyPairScheme = new Ed25519Pair();
    private final Base64URLData encodedPrivateUrl = new Base64URLData(keyPairScheme.scalar.toBytes());
    private final ElectionPrivateKey validDecryptionScheme =
            new ElectionPrivateKey(encodedPrivateUrl);

    @Test
    public void constructorTest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ElectionPublicKey(nonValidMockEncodedElectionKey));
        assertNotEquals(null, validDecryptionScheme);
    }

    @Test
    public void toStringTest() {
        String format = keyPairScheme.scalar.toString();
        assertEquals(format, validDecryptionScheme.getPrivateKey().toString());
    }

    @Test
    public void equalsTest() {
        ElectionKeyPair scheme = ElectionKeyPair.generateKeyPair();
        ElectionPrivateKey that = scheme.getDecryptionScheme();
        assertEquals(validDecryptionScheme, validDecryptionScheme);
        assertNotEquals(that, validDecryptionScheme);
        assertNotEquals(null, validDecryptionScheme);
        int hash = java.util.Objects.hash(keyPairScheme.scalar.toString());
        assertEquals(hash, validDecryptionScheme.hashCode());
    }

    // Encryption / decryption process is already tested in ElectionKeyPairTest
    // We check that encryption with wrong format argument throws the appropriate exception
    @Test
    public void decryptTest() {
        String wrongBase64Encoding = "123";
        assertThrows(IllegalArgumentException.class,
                () -> validDecryptionScheme.decrypt(wrongBase64Encoding));

        String wrongLengthMessage = "LX-_Hw==";
        assertThrows(IllegalArgumentException.class,
                () -> validDecryptionScheme.decrypt(wrongLengthMessage));

    }
}