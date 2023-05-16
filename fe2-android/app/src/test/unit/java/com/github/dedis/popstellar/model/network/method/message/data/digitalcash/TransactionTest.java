package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
import com.github.dedis.popstellar.model.objects.security.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TransactionTest {
  // Version
  private static final int VERSION = 1;

  // Creation TxOut
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";
  private static final ScriptInput SCRIPTTXIN =
      new ScriptInput(TYPE, new PublicKey(PUBKEY), new Signature(SIG));
  private static final Input TXIN = new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  // Creation TXOUT
  private static final int VALUE = 32;
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";
  private static final ScriptOutput SCRIPT_TX_OUT = new ScriptOutput(TYPE, PUBKEYHASH);
  private static final Output TXOUT = new Output(VALUE, SCRIPT_TX_OUT);

  // List TXIN, List TXOUT
  private static final List<Input> TX_INS = Collections.singletonList(TXIN);
  private static final List<Output> TX_OUTS = Collections.singletonList(TXOUT);

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
    assertEquals(TX_INS, TRANSACTION.getInputs());
  }

  @Test
  public void testGetTxOuts() {
    assertEquals(TX_OUTS, TRANSACTION.getOutputs());
  }

  @Test
  public void testGetTimestamp() {
    assertEquals(TIMESTAMP, TRANSACTION.getLockTime());
  }

  @Test
  public void testEquals() {
    assertEquals(TRANSACTION, new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP));
    assertNotEquals(TRANSACTION, new Transaction(4, TX_INS, TX_OUTS, TIMESTAMP));
    assertNotEquals(TRANSACTION, new Transaction(VERSION, TX_INS, TX_OUTS, 6));

    Input WrongTxIn = new Input("random", TX_OUT_INDEX, SCRIPTTXIN);
    List<Input> WrongListTxin = Collections.singletonList(WrongTxIn);
    assertNotEquals(TRANSACTION, new Transaction(VERSION, WrongListTxin, TX_OUTS, TIMESTAMP));

    Output WrongTxOut = new Output(4, SCRIPT_TX_OUT);
    List<Output> WrongListTxout = Collections.singletonList(WrongTxOut);
    assertNotEquals(TRANSACTION, new Transaction(VERSION, TX_INS, WrongListTxout, TIMESTAMP));
  }

  @Test
  public void computeIdTest() {
    String path = "protocol/examples/messageData/coin/post_transaction_coinbase.json";
    String validJson = JsonUtilsTest.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonUtilsTest.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();

    assertEquals(postTransactionModel.getTransactionId(), transactionModel.computeId());
  }

  @Test
  public void verifySignature() {
    String path = "protocol/examples/messageData/coin/post_transaction_coinbase.json";
    String validJson = JsonUtilsTest.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonUtilsTest.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();
    Input single = transactionModel.getInputs().get(0);

    Signature sig = single.getScript().getSig();
    PublicKey pk = single.getScript().getPubkey();

    assertTrue(
        pk.verify(
            sig,
            new Base64URLData(
                Transaction.computeSigOutputsPairTxOutHashAndIndex(
                        transactionModel.getOutputs(),
                        Collections.singletonMap(single.getTxOutHash(), single.getTxOutIndex()))
                    .getBytes(StandardCharsets.UTF_8))));

    path = "protocol/examples/messageData/coin/post_transaction_bad_signature.json";
    validJson = JsonUtilsTest.loadFile(path);
    postTransactionModel = (PostTransactionCoin) JsonUtilsTest.parse(validJson);
    transactionModel = postTransactionModel.getTransaction();
    single = transactionModel.getInputs().get(0);

    sig = single.getScript().getSig();
    pk = single.getScript().getPubkey();

    assertFalse(
        pk.verify(
            sig,
            new Base64URLData(
                Transaction.computeSigOutputsPairTxOutHashAndIndex(
                        transactionModel.getOutputs(),
                        Collections.singletonMap(single.getTxOutHash(), single.getTxOutIndex()))
                    .getBytes(StandardCharsets.UTF_8))));
  }
}
