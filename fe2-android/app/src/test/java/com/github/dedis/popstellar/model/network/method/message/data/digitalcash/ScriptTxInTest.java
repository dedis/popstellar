package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ScriptTxInTest {
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";

  private static final ScriptTxIn SCRIPTTXIN = new ScriptTxIn(TYPE, PUBKEY, SIG);

  @Test
  public void testGetSig() {
    assertEquals(SIG, SCRIPTTXIN.getSig());
  }

  @Test
  public void testGetType() {
    assertEquals(TYPE, SCRIPTTXIN.getType());
  }

  @Test
  public void testGetPub_key_recipient() {
    assertEquals(PUBKEY, SCRIPTTXIN.getPub_key_recipient());
  }

  @Test
  public void testTestEquals() {
    assertEquals(SCRIPTTXIN, new ScriptTxIn(TYPE, PUBKEY, SIG));
    String random = "random";
    assertNotEquals(SCRIPTTXIN, new ScriptTxIn(random, PUBKEY, SIG));
    assertNotEquals(SCRIPTTXIN, new ScriptTxIn(TYPE, random, SIG));
    assertNotEquals(SCRIPTTXIN, new ScriptTxIn(TYPE, PUBKEY, random));
  }
}
