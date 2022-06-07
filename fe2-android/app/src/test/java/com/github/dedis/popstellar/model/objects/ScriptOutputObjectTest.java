package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

public class ScriptOutputObjectTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  public static final String TYPE = "P2PKH";
  public static String PUBKEYHASH = SENDER.computeHash();
  private static ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);

  @Test
  public void getTypeTest() {
    assertEquals(TYPE, SCRIPTTXOUT.getType());
  }

  @Test
  public void getPublicKeyHashTest() {
    assertEquals(PUBKEYHASH, SCRIPTTXOUT.getPubKeyHash());
  }
}
