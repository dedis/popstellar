package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class Script_inputTest {
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";

  private static final Script_input SCRIPTTXIN = new Script_input(TYPE, PUBKEY, SIG);

  @Test
  public void testGetType() {
    assertEquals(TYPE, SCRIPTTXIN.get_type());
  }

  @Test
  public void testGetSig() {
    assertEquals(SIG, SCRIPTTXIN.get_sig());
  }

  @Test
  public void testGetPub_key_recipient() {
    assertEquals(PUBKEY, SCRIPTTXIN.get_pubkey());
  }

  @Test
  public void testTestEquals() {
    assertEquals(SCRIPTTXIN, new Script_input(TYPE, PUBKEY, SIG));
    String random = "random";
    assertNotEquals(SCRIPTTXIN, new Script_input(random, PUBKEY, SIG));
    assertNotEquals(SCRIPTTXIN, new Script_input(TYPE, random, SIG));
    assertNotEquals(SCRIPTTXIN, new Script_input(TYPE, PUBKEY, random));
  }
}
