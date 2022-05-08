package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ScriptTxOutTest {
  public static final String TYPE = "P2PKH";
  public static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  public static final ScriptTxOut SCRIPTTXOUT = new ScriptTxOut(TYPE, PUBKEYHASH);

  @Test
  public void testGetType() {
    assertEquals(TYPE, SCRIPTTXOUT.getType());
  }

  @Test
  public void testGetPub_key_hash() {
    assertEquals(PUBKEYHASH, SCRIPTTXOUT.getPub_key_hash());
  }

  @Test
  public void testEquals() {
    assertEquals(SCRIPTTXOUT, new ScriptTxOut(TYPE, PUBKEYHASH));
    String random = "random";
    assertNotEquals(SCRIPTTXOUT, new ScriptTxOut(random, PUBKEYHASH));
    assertNotEquals(SCRIPTTXOUT, new ScriptTxOut(TYPE, random));
  }
}
