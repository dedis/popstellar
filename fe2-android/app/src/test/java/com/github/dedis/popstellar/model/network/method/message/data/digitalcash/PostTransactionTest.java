package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class PostTransactionTest {
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

  // POST TRANSACTION
  private static final PostTransaction POST_TRANSACTION = new PostTransaction(TRANSACTION);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.TRANSACTION.getObject(), POST_TRANSACTION.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.POST.getAction(), POST_TRANSACTION.getAction());
  }

  @Test
  public void getTransactionTest() {
    assertEquals(POST_TRANSACTION.getTransaction(), TRANSACTION);
  }

}
