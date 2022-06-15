package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.util.Base64;
import android.util.Log;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.objects.InputObject;
import com.github.dedis.popstellar.model.objects.OutputObject;
import com.github.dedis.popstellar.model.objects.ScriptInputObject;
import com.github.dedis.popstellar.model.objects.ScriptOutputObject;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PrivateKey;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TransactionTest {
  // Version
  private static final int VERSION = 1;

  // Creation TxOut
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";
  private static final ScriptInput SCRIPTTXIN = new ScriptInput(TYPE, new PublicKey(PUBKEY), new Signature(SIG));
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
    String validJson = JsonTestUtils.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonTestUtils.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();

    assertEquals(postTransactionModel.getTransactionId(), transactionModel.computeId());
  }

  @Test
  public void computeSignature() throws GeneralSecurityException {
    String path = "protocol/examples/messageData/coin/post_transaction_coinbase.json";
    String validJson = JsonTestUtils.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonTestUtils.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();
    Input single = transactionModel.getInputs().get(0);
    //Signature sig = single.getScript().getSig();
    //PublicKey pk = single.getScript().getPubkey();
    //System.out.println(Transaction.computeSigOutputsPairTxOutHashAndIndex(transactionModel.getOutputs(), Collections.singletonMap(single.getTxOutHash(), single.getTxOutIndex())).getBytes(StandardCharsets.UTF_8));
    KeyPair kp1 = Base64DataUtils.generateKeyPair();
    KeyPair kp2 = Base64DataUtils.generateKeyPair();

    //OUTPUT
    Output singleo = transactionModel.getOutputs().get(0);
    transactionModel.getOutputs().remove(0);
    Output newo = new Output(32, new ScriptOutput(singleo.getScript().getType(), kp2.getPublicKey().computeHash()));
    transactionModel.getOutputs().add(newo);

    //INPUT
    String signTxt = Transaction.computeSigOutputsPairTxOutHashAndIndex(
            Collections.singletonList(newo), Collections.singletonMap(single.getTxOutHash(), single.getTxOutIndex()));
    Base64URLData data = new Base64URLData(signTxt.getBytes(StandardCharsets.UTF_8));
    Signature sig = kp1.sign(data);

    transactionModel.getInputs().remove(0);
    Input newOne = new Input(single.getTxOutHash(), single.getTxOutIndex(), new ScriptInput(single.getScript().getType(), kp1.getPublicKey(), sig));
    transactionModel.getInputs().add(newOne);
    System.out.println(postTransactionModel.toString());
    System.out.println(kp1.toString());
    System.out.println(kp2.toString());
    System.out.println("TextToBeSigned: "+signTxt);
    System.out.println("Encoded Base64 bef sign: "+data.getEncoded());
    System.out.println("kp1: "+transactionModel.getInputs().get(0).getScript().getPubkey().verify(sig, data));

    //assertTrue(pk.verify(sig, new Base64URLData(Transaction.computeSigOutputsPairTxOutHashAndIndex(transactionModel.getOutputs(), Collections.singletonMap(single.getTxOutHash(), single.getTxOutIndex())).getBytes())));
  }

}
