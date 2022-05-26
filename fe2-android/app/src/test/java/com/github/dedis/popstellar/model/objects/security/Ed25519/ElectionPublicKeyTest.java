package com.github.dedis.popstellar.model.objects.security.Ed25519;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ElectionPublicKeyTest {


    private final ElectionKeyPair mockEncodedKey =
            ElectionKeyPair.generateKeyPair();

    private final String mockElectionKeyString = "uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ=";
    private final Base64URLData mockEncodedElectionKey = new Base64URLData(mockElectionKeyString.getBytes(StandardCharsets.UTF_8));
    ElectionPublicKey mockElectionKey = new ElectionPublicKey(mockEncodedElectionKey);

    @Test
    public void toStringTest() {
        System.out.println(mockElectionKey.toString());
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void toBase64() {
    }

    @Test
    public void encrypt() {
    }
}