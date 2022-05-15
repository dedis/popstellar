package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class Script_outputTest {
  public static final String TYPE = "P2PKH";
  public static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  public static final Script_output SCRIPTTXOUT = new Script_output(TYPE, PUBKEYHASH);

  @Test
  public void testGetType() {
    assertEquals(TYPE, SCRIPTTXOUT.get_type());
  }

  @Test
  public void testGetPub_key_hash() {
    assertEquals(PUBKEYHASH, SCRIPTTXOUT.get_pubkey_hash());
  }

  @Test
  public void testEquals() {
    assertEquals(SCRIPTTXOUT, new Script_output(TYPE, PUBKEYHASH));
    String random = "random";
    assertNotEquals(SCRIPTTXOUT, new Script_output(random, PUBKEYHASH));
    assertNotEquals(SCRIPTTXOUT, new Script_output(TYPE, random));
  }
}
