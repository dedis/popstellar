package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class OutputTest {
  private static final int VALUE = 32;

  private static final String TYPE = "P2PKH";
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";

  private static final Script_output SCRIPT_TX_OUT = new Script_output(TYPE, PUBKEYHASH);

  private static final Output TXOUT = new Output(VALUE, SCRIPT_TX_OUT);

  @Test
  public void testGetValue() {
    assertEquals(VALUE, TXOUT.get_value());
  }

  @Test
  public void testGetScript() {
    assertEquals(SCRIPT_TX_OUT, TXOUT.get_script());
  }

  @Test
  public void testEquals() {
    assertEquals(TXOUT, new Output(VALUE, SCRIPT_TX_OUT));
    String random = "random";
    assertNotEquals(TXOUT, new Output(4, SCRIPT_TX_OUT));
    assertNotEquals(TXOUT, new Output(VALUE, new Script_output(random, PUBKEYHASH)));
  }
}
