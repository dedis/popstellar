package com.github.dedis.popstellar.model.objects.security.ed25519;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import ch.epfl.dedis.lib.crypto.Ed25519Pair;
import ch.epfl.dedis.lib.crypto.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class ElectionPublicKeyTest {

  private final String nonValidMockElectionKeyString =
      "uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ=";
  private final Base64URLData nonValidMockEncodedElectionKey =
      new Base64URLData(nonValidMockElectionKeyString.getBytes(StandardCharsets.UTF_8));

  private final Ed25519Pair keyPairScheme = new Ed25519Pair();
  private final Base64URLData encodedPublicUrl = new Base64URLData(keyPairScheme.point.toBytes());
  private final ElectionPublicKey validEncryptionScheme = new ElectionPublicKey(encodedPublicUrl);

  @Test
  public void constructorTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ElectionPublicKey(nonValidMockEncodedElectionKey));
    assertNotEquals(null, validEncryptionScheme);
  }

  @Test
  public void toStringTest() {
    String format = keyPairScheme.point.toString();
    assertEquals(format, validEncryptionScheme.toString());
  }

  @Test
  public void equalsTest() {
    ElectionKeyPair scheme = ElectionKeyPair.generateKeyPair();
    ElectionPublicKey that = scheme.getEncryptionScheme();
    assertNotEquals(validEncryptionScheme, that);
    assertEquals(validEncryptionScheme, validEncryptionScheme);
    assertNotEquals(null, validEncryptionScheme);
    int hash = java.util.Objects.hash(keyPairScheme.point.toString());
    assertEquals(hash, validEncryptionScheme.hashCode());
  }

  @Test
  public void toBase64Test() {
    Base64URLData keyPoint = new Base64URLData(keyPairScheme.point.toBytes());
    assertEquals(keyPoint, validEncryptionScheme.toBase64());
  }

  @Test
  public void encodeToBase64Test() {
    Base64URLData keyPoint = new Base64URLData(keyPairScheme.point.toBytes());
    String encoded = keyPoint.getEncoded();
    assertEquals(encoded, validEncryptionScheme.encodeToBase64());
  }

  @Test
  public void getPublicKeyTest() {
    Point keyPoint = keyPairScheme.point;
    assertEquals(keyPoint, validEncryptionScheme.getPublicKey());
  }

  // Encryption / decryption process is already tested in ElectionKeyPairTest
  // We check that encryption with wrong format argument throws the appropriate exception
  @Test
  public void encryptTest() {
    // Message should not exceed 29 bytes
    byte[] tooLongMessage = new byte[30];
    assertThrows(
        IllegalArgumentException.class, () -> validEncryptionScheme.encrypt(tooLongMessage));
  }
}
