package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ScriptOutputTest {
  public static final String TYPE = "P2PKH";
  public static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  public static final ScriptOutput SCRIPTTXOUT = new ScriptOutput(TYPE, PUBKEYHASH);

  @Test
  public void testGetType() {
    assertEquals(TYPE, SCRIPTTXOUT.getType());
  }

  @Test
  public void testGetPub_key_hash() {
    assertEquals(PUBKEYHASH, SCRIPTTXOUT.getPubkeyHash());
  }

  @Test
  public void testEquals() {
    assertEquals(SCRIPTTXOUT, new ScriptOutput(TYPE, PUBKEYHASH));
    String random = "random";
    assertNotEquals(SCRIPTTXOUT, new ScriptOutput(random, PUBKEYHASH));
    assertNotEquals(SCRIPTTXOUT, new ScriptOutput(TYPE, random));
  }
}
