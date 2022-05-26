package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class InputTest {
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";

  private static final Script_input SCRIPTTXIN = new Script_input(TYPE, PUBKEY, SIG);

  private static final Input TXIN = new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  @Test
  public void testGetTxOutHash() {
    assertEquals(Tx_OUT_HASH, TXIN.get_tx_out_hash());
  }

  @Test
  public void testGetTxOutIndex() {
    assertEquals(TX_OUT_INDEX, TXIN.get_tx_out_index());
  }

  @Test
  public void testGetScript() {
    assertEquals(SCRIPTTXIN, TXIN.get_script());
  }

  @Test
  public void testTestEquals() {
    assertEquals(TXIN, new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN));
    String random = "random";
    assertNotEquals(TXIN, new Input(random, TX_OUT_INDEX, SCRIPTTXIN));
    assertNotEquals(TXIN, new Input(Tx_OUT_HASH, 4, SCRIPTTXIN));
    assertNotEquals(
        TXIN, new Input(Tx_OUT_HASH, TX_OUT_INDEX, new Script_input(random, PUBKEY, SIG)));
  }
}
