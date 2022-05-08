package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class TransactionTest {
  // Version
  private static final int VERSION = 1;

  // Creation TxOut
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";
  private static final ScriptTxIn SCRIPTTXIN = new ScriptTxIn(TYPE, PUBKEY, SIG);
  private static final TxIn TXIN = new TxIn(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  // Creation TXOUT
  private static final int VALUE = 32;
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";
  private static final ScriptTxOut SCRIPT_TX_OUT = new ScriptTxOut(TYPE, PUBKEYHASH);
  private static final TxOut TXOUT = new TxOut(VALUE, SCRIPT_TX_OUT);

  // List TXIN, List TXOUT
  private static final List<TxIn> TX_INS = Collections.singletonList(TXIN);
  private static final List<TxOut> TX_OUTS = Collections.singletonList(TXOUT);

  // Locktime
  private static final long TIMESTAMP = 0;

  // Transaction
  private static final Transaction TRANSACTION =
      new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP);

  @Test
  public void testGetVersion() {
    assertEquals(VERSION, TRANSACTION.getVersion());
  }

  @Test
  public void testGetTxIns() {
    assertEquals(TX_INS, TRANSACTION.getTxIns());
  }

  @Test
  public void testGetTxOuts() {
    assertEquals(TX_OUTS, TRANSACTION.getTxOuts());
  }

  @Test
  public void testGetTimestamp() {
    assertEquals(TIMESTAMP, TRANSACTION.getTimestamp());
  }

  @Test
  public void testEquals() {
    assertEquals(TRANSACTION, new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP));
    assertNotEquals(TRANSACTION, new Transaction(4, TX_INS, TX_OUTS, TIMESTAMP));
    assertNotEquals(TRANSACTION, new Transaction(VERSION, TX_INS, TX_OUTS, 6));

    TxIn WrongTxIn = new TxIn("random", TX_OUT_INDEX, SCRIPTTXIN);
    List<TxIn> WrongListTxin = Collections.singletonList(WrongTxIn);
    assertNotEquals(TRANSACTION, new Transaction(VERSION, WrongListTxin, TX_OUTS, TIMESTAMP));

    TxOut WrongTxOut = new TxOut(4, SCRIPT_TX_OUT);
    List<TxOut> WrongListTxout = Collections.singletonList(WrongTxOut);
    assertNotEquals(TRANSACTION, new Transaction(VERSION, TX_INS, WrongListTxout, TIMESTAMP));
  }

  @Test
  public void testComputeId() {
    String result = TRANSACTION.computeId();
    String expected = "dBGU54vni3deHEebvJC2LcZbm0chV1GrJDGfMlJSLRc=";
    assertEquals(result, expected);
  }
}
