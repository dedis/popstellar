package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class OutputTest {
  private static final int VALUE = 32;

  private static final String TYPE = "P2PKH";
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  private static final ScriptOutput SCRIPT_TX_OUT = new ScriptOutput(TYPE, PUBKEYHASH);

  private static final Output TXOUT = new Output(VALUE, SCRIPT_TX_OUT);

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
    assertEquals(TXOUT, new Output(VALUE, SCRIPT_TX_OUT));
    String random = "random";
    assertNotEquals(TXOUT, new Output(4, SCRIPT_TX_OUT));
    assertNotEquals(TXOUT, new Output(VALUE, new ScriptOutput(random, PUBKEYHASH)));
  }
}
