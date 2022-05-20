package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

public class Script_output_objectTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  public static final String TYPE = "P2PKH";
  public static String PUBKEYHASH = SENDER.computeHash();
  private static Script_output_object SCRIPTTXOUT = new Script_output_object(TYPE, PUBKEYHASH);

  @Test
  public void get_typeTest() {
    assertEquals(TYPE, SCRIPTTXOUT.get_type());
  }

  @Test
  public void get_publicKeyHashTest() {
    assertEquals(PUBKEYHASH, SCRIPTTXOUT.get_pubkey_hash());
  }
}
