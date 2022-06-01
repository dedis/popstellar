package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;

import org.junit.Test;

public class InputTest {
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";

  private static final ScriptInput SCRIPTTXIN = new ScriptInput(TYPE, new PublicKey(PUBKEY), new Signature(SIG));

  private static final Input TXIN = new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  @Test
  public void testGetTxOutHash() {
    assertEquals(Tx_OUT_HASH, TXIN.getTxOutHash());
  }

  @Test
  public void testGetTxOutIndex() {
    assertEquals(TX_OUT_INDEX, TXIN.getTxOutIndex());
  }

  @Test
  public void testGetScript() {
    assertEquals(SCRIPTTXIN, TXIN.getScript());
  }

  @Test
  public void testTestEquals() {
    assertEquals(TXIN, new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN));
    String random = "random";
    assertNotEquals(TXIN, new Input(random, TX_OUT_INDEX, SCRIPTTXIN));
    assertNotEquals(TXIN, new Input(Tx_OUT_HASH, 4, SCRIPTTXIN));
    assertNotEquals(
        TXIN, new Input(Tx_OUT_HASH, TX_OUT_INDEX, new ScriptInput(random, new PublicKey(PUBKEY), new Signature(SIG))));
  }
}
