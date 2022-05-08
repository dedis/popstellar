package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TxOutTest {
  private static final int VALUE = 32;

  private static final String TYPE = "P2PKH";
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  private static final ScriptTxOut SCRIPT_TX_OUT = new ScriptTxOut(TYPE, PUBKEYHASH);

  private static final TxOut TXOUT = new TxOut(VALUE, SCRIPT_TX_OUT);

  @Test
  public void testGetValue() {
    assertEquals(VALUE, TXOUT.getValue());
  }

  @Test
  public void testGetScript() {
    assertEquals(SCRIPT_TX_OUT, TXOUT.getScript());
  }

  @Test
  public void testEquals() {
    assertEquals(TXOUT, new TxOut(VALUE, SCRIPT_TX_OUT));
    String random = "random";
    assertNotEquals(TXOUT, new TxOut(4, SCRIPT_TX_OUT));
    assertNotEquals(TXOUT, new TxOut(VALUE, new ScriptTxOut(random, PUBKEYHASH)));
  }
}
