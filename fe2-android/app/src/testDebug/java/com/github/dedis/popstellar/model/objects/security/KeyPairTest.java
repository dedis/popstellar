package com.github.dedis.popstellar.model.objects.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Test;

import java.security.GeneralSecurityException;

public class KeyPairTest {

  private static final Signature SIGNATURE = new Signature("U0lHTkFUVVJF");
  private static final Base64URLData DATA = new Base64URLData("REFUQQ==");

  @Test
  public void signDataUsesSignFromThePrivateKey() throws GeneralSecurityException {
    PrivateKey privateKey = mock(PrivateKey.class);
    when(privateKey.sign(any())).thenReturn(SIGNATURE);

    PublicKey publicKey = mock(PublicKey.class);
    when(publicKey.verify(any(), any())).thenReturn(true);

    KeyPair pair = new KeyPair(privateKey, publicKey);
    assertTrue(pair.verify(SIGNATURE, DATA));
    assertEquals(SIGNATURE, pair.sign(DATA));

    verify(privateKey).sign(any());
    verify(publicKey).verify(any(), any());
  }

  @Test
  public void getPublicKeyReturnsRightValue() {
    PrivateKey privateKey = mock(PrivateKey.class);
    PublicKey publicKey =
        new PublicKey(
            Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7"));
    KeyPair keyPair = new KeyPair(privateKey, publicKey);

    assertEquals(publicKey, keyPair.getPublicKey());
  }
}
