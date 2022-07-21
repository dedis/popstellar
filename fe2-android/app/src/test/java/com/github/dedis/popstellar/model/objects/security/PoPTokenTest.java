package com.github.dedis.popstellar.model.objects.security;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PoPTokenTest {

  private static final Base64URLData DATA = new Base64URLData("REFUQQ==");
  private static final Signature BAD_SIGNATURE = new Signature("U0lHTkFUVVJF");

  private static final byte[] VALID_PRIVATE_KEY =
      Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66");
  private static final byte[] VALID_PUBLIC_KEY =
      Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7");

  private static final byte[] VALID_PRIVATE_KEY2 =
      Utils.hexToBytes("cf74d353042400806ee94c3e77eef983d9a1434d21c0a7568f203f5b091dde1d");
  private static final byte[] VALID_PUBLIC_KEY2 =
      Utils.hexToBytes("6015ae4d770294f94e651a9fd6ba9c6a11e5c80803c63ee472ad525f4c3523a6");

  @Test
  public void signAndVerifyWorks() throws GeneralSecurityException {
    PoPToken pair = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);
    Signature signing = pair.sign(DATA);
    assertTrue(pair.verify(signing, DATA));
  }

  @Test
  public void badSignatureFails() {
    PoPToken pair = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);
    assertFalse(pair.verify(BAD_SIGNATURE, DATA));
  }

  @Test
  public void equalsAndHashcodeWorksWhenSame() {
    PoPToken token1 = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);
    PoPToken token2 = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);

    assertEquals(token1, token2);
    assertEquals(token1.hashCode(), token2.hashCode());
  }

  @Test
  public void equalsAndHashcodeWorksWhenDifferent() {
    PoPToken token1 = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);
    PoPToken token2 = new PoPToken(VALID_PRIVATE_KEY2, VALID_PUBLIC_KEY2);

    assertNotEquals(token1, token2);
    assertNotEquals(token1.hashCode(), token2.hashCode());
  }

  @Test
  public void equalsSpecialCases() {
    PoPToken token = new PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY);
    assertNotEquals(token, null);
  }
}
