package com.github.dedis.popstellar.model.objects.security.Ed25519;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ElectionKeyPairTest {

    private static final char VALUE = 'x';
    private static final ElectionKeyPair KEY = ElectionKeyPair.generateKeyPair();

    @Test
    public void generateKeyPairTest() {
        // Should not crash upon generation
        assertNotEquals(null, KEY.decryptionScheme);
        assertNotEquals(null, KEY.encryptionScheme);
    }


}