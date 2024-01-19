package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostTransactionCoinTest {
  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.COIN.getObject(), POST_TRANSACTION.getObject())
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.POST_TRANSACTION.action, POST_TRANSACTION.action)
  }

  @Test
  fun transactionTest() {
    Assert.assertEquals(TRANSACTION, POST_TRANSACTION.transaction)
  }

  @Test
  fun transactionIdTest() {
    val expected = "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc="
    Assert.assertEquals(expected, POST_TRANSACTION.transactionId)
  }

  @Test
  fun jsonValidationTest() {
    val GSON = provideGson(buildRegistry())
    val json = GSON.toJson(POST_TRANSACTION, Data::class.java)

    JsonUtils.verifyJson("protocol/query/method/message/data/dataPostTransactionCoin.json", json)

    val res = GSON.fromJson(json, PostTransactionCoin::class.java)
    Assert.assertEquals(POST_TRANSACTION, res)
  }

  @Test
  fun testHashCode() {
    val trans = Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP)
    val postTransaction = PostTransactionCoin(trans)

    Assert.assertEquals(
      java.util.Objects.hash(postTransaction.transactionId, postTransaction.transaction).toLong(),
      postTransaction.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val trans = Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP)
    val postTransaction = PostTransactionCoin(trans)

    Assert.assertEquals(
      "PostTransactionCoin{ transaction_id=_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=, transaction=Transaction{version=1, inputs=[input{tx_out_hash='47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=', tx_out_index=0, script=script{type='P2PKH', pubkey='PublicKey(AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=)', sig='Signature(CAFEBABE)'}}], outputs=[output{value=32, script=script{type='P2PKH', pubkey_hash='2jmj7l5rSw0yVb-vlWAYkK-YBwk='}}], lock_time=0}}",
      postTransaction.toString()
    )
  }

  companion object {
    // Version
    private const val VERSION = 1

    // Creation TxOut
    private const val TX_OUT_INDEX = 0
    private const val Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    private const val TYPE = "P2PKH"
    private const val PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val SIG = "CAFEBABE"
    private val SCRIPTTXIN = ScriptInput(TYPE, PublicKey(PUBKEY), Signature(SIG))
    private val TXIN = Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN)

    // Creation TXOUT
    private const val VALUE = 32
    private const val PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
    private val SCRIPT_TX_OUT = ScriptOutput(TYPE, PUBKEYHASH)
    private val TXOUT = Output(VALUE.toLong(), SCRIPT_TX_OUT)

    // List TXIN, List TXOUT
    private val TX_INS = listOf(TXIN)
    private val TX_OUTS = listOf(TXOUT)

    // Locktime
    private const val TIMESTAMP: Long = 0

    // Transaction
    private val TRANSACTION = Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP)

    // POST TRANSACTION
    private val POST_TRANSACTION = PostTransactionCoin(TRANSACTION)
    private val FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
  }
}
