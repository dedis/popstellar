package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrivateKeyTest {

  private static final Base64URLData DATA = new Base64URLData("REFUQQ==");
  private static final byte[] VALID_PRIVATE_KEY =
      Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66");
  private static final Signature EXPECTED_SIGNATURE =
      new Signature(
          "hhJwFWUwcm1B9PapIQ6Ct6NDRBpITP_AGsIHaU6biJ8d94uDEydGrRZ5NInIjwBqoqUa2rROgx0xA705pXkgDQ==");

  @Test
  public void signGivesSameValueForBothKeyType() throws GeneralSecurityException {
    PrivateKey key1 = new PlainPrivateKey(VALID_PRIVATE_KEY);

    KeysetHandle keyset = mock(KeysetHandle.class);
    when(keyset.getPrimitive(PublicKeySign.class)).thenReturn(new Ed25519Sign(VALID_PRIVATE_KEY));
    PrivateKey key2 = new ProtectedPrivateKey(keyset);

    Signature sign1 = key1.sign(DATA);
    Signature sign2 = key2.sign(DATA);

    assertEquals(sign1, sign2);
  }

  @Test
  public void signGivesExpectedValue() throws GeneralSecurityException {
    PrivateKey key = new PlainPrivateKey(VALID_PRIVATE_KEY);

    Signature sign = key.sign(DATA);
    assertEquals(EXPECTED_SIGNATURE, sign);
  }

  @Test
  public void badKeyFailsAtConstruction() throws GeneralSecurityException {
    assertThrows(IllegalArgumentException.class, () -> new PlainPrivateKey(new byte[] {0, 1, 2}));

    KeysetHandle keyset = mock(KeysetHandle.class);
    when(keyset.getPrimitive(PublicKeySign.class)).thenThrow(new GeneralSecurityException());
    assertThrows(IllegalArgumentException.class, () -> new ProtectedPrivateKey(keyset));
  }

  @Test
  public void privateKeyHidesValueInStringRepresentation() {
    PlainPrivateKey key = new PlainPrivateKey(VALID_PRIVATE_KEY);

    assertFalse(key.toString().contains(key.getEncoded()));
  }
}
