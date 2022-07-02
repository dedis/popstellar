package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.google.gson.Gson;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class PostTransactionCoinTest {
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

  // POST TRANSACTION
  private static final PostTransactionCoin POST_TRANSACTION = new PostTransactionCoin(TRANSACTION);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.COIN.getObject(), POST_TRANSACTION.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.POST_TRANSACTION.getAction(), POST_TRANSACTION.getAction());
  }

  @Test
  public void getTransactionTest() {
    assertEquals(TRANSACTION, POST_TRANSACTION.getTransaction());
  }

  @Test
  public void getTransactionIdTest() {
    String expected = "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=";
    assertEquals(expected, POST_TRANSACTION.getTransactionId());
  }

  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

  @Test
  public void jsonValidationTest() {
    Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
    String json = GSON.toJson(POST_TRANSACTION, Data.class);
    JsonUtils.verifyJson("protocol/query/method/message/data/dataPostTransactionCoin.json", json);
    PostTransactionCoin res = GSON.fromJson(json, PostTransactionCoin.class);
    assertEquals(POST_TRANSACTION, res);
  }

  @Test
  public void testHashCode() {
    Transaction trans = new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP);
    PostTransactionCoin postTransaction = new PostTransactionCoin(trans);
    assertEquals(java.util.Objects.hash(postTransaction.getTransactionId(), postTransaction.getTransaction()), postTransaction.hashCode());
  }

  @Test
  public void testToString() {
    Transaction trans = new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP);
    PostTransactionCoin postTransaction = new PostTransactionCoin(trans);
    assertEquals("PostTransactionCoin{ transaction_id=_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=, transaction=Transaction{version=1, inputs=[input{tx_out_hash='47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=', tx_out_index=0, script=script{type='P2PKH', pubkey='PublicKey(AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=)', sig='Signature(CAFEBABE)'}}], outputs=[output{value=32, script=script{type='P2PKH', pubkey_hash='2jmj7l5rSw0yVb-vlWAYkK-YBwk='}}], lock_time=0}}", postTransaction.toString());
  }

}
