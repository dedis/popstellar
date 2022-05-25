package com.github.dedis.popstellar.model.objects.security.Ed25519;

import org.junit.Test;

public class ElectionPublicKeyTest {


    @Test
    public void testToString() {

        ElectionKeyPair keys = ElectionKeyPair.generateKeyPair();
        ElectionPublicKey publicKey = keys.getEncryptionScheme();
        ElectionPrivateKey privateKey = keys.getDecryptionScheme();

        int val = 2;
        byte[] voteIndiceInBytes = {(byte) val, (byte) (val >> 8)};
        String encrypted = publicKey.encrypt(voteIndiceInBytes);
        System.out.println(encrypted);
        try {
            privateKey.decrypt(encrypted);
            System.out.println(val);
        } catch (Exception e) {
            System.out.println("eee");
        }
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