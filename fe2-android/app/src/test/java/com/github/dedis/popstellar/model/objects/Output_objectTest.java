package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

public class Output_objectTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  public static final String TYPE = "P2PKH";
  public static String PUBKEYHASH = SENDER.computeHash();
  private static ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);

  private static final int VALUE = 32;
  private static final OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);

  @Test
  public void get_valueTest() {
    assertEquals(VALUE, OUTPUT.getValue());
  }

  @Test
  public void get_scriptTest() {
    assertEquals(TYPE, OUTPUT.getScript().getType());
    assertEquals(PUBKEYHASH, OUTPUT.getScript().getPubkeyHash());
  }
}
