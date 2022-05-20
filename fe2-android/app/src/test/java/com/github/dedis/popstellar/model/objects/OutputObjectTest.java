package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

public class OutputObjectTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  public static final String TYPE = "P2PKH";
  public static String PUBKEYHASH = SENDER.computeHash();
  private static Script_output_object SCRIPTTXOUT = new Script_output_object(TYPE, PUBKEYHASH);

  private static final int VALUE = 32;
  private static final Output_object OUTPUT = new Output_object(VALUE, SCRIPTTXOUT);

  @Test
  public void getValueTest() {
    assertEquals(VALUE, OUTPUT.get_value());
  }

  @Test
  public void getScriptTest() {
    assertEquals(TYPE, OUTPUT.get_script().get_type());
    assertEquals(PUBKEYHASH, OUTPUT.get_script().get_pubkey_hash());
  }
}
