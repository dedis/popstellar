package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.gson.Gson;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

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

  @Test
  public void getTransactionIdTest() {
    String expected = "dBGU54vni3deHEebvJC2LcZbm0chV1GrJDGfMlJSLRc=";
    assertEquals(expected, POST_TRANSACTION.getTransaction_id());
  }

  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

  @Test
  public void jsonValidationTest() {
    Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
    String json = GSON.toJson(POST_TRANSACTION, Data.class);
    JsonUtils.verifyJson("protocol/query/method/message/data/dataCashTransaction.json", json);
    PostTransaction res = GSON.fromJson(json, PostTransaction.class);
    assertEquals(POST_TRANSACTION, res);
  }
}
