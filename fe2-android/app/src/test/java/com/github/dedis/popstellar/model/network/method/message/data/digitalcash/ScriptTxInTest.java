package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import junit.framework.TestCase;

public class ScriptTxInTest extends TestCase {
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";

  private static final ScriptTxIn SCRIPTTXIN = new ScriptTxIn(TYPE, PUBKEY, SIG);

  public void testGetSig() {
    assertEquals(SIG, SCRIPTTXIN.getSig());
  }

  public void testGetType() {}

  public void testGetPub_key_recipient() {}

  public void testTestEquals() {}

  public void testTestHashCode() {}

  public void testTestToString() {}
}
