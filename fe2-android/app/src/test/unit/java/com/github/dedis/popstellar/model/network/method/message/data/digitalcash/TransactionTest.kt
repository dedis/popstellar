package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import java.nio.charset.StandardCharsets
import java.util.Collections
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionTest {
  @Test
  fun testGetVersion() {
    Assert.assertEquals(VERSION.toLong(), TRANSACTION.version.toLong())
  }

  @Test
  fun testGetTxIns() {
    Assert.assertEquals(TX_INS, TRANSACTION.inputs)
  }

  @Test
  fun testGetTxOuts() {
    Assert.assertEquals(TX_OUTS, TRANSACTION.outputs)
  }

  @Test
  fun testGetTimestamp() {
    Assert.assertEquals(TIMESTAMP, TRANSACTION.lockTime)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(TRANSACTION, Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP))

    Assert.assertNotEquals(TRANSACTION, Transaction(4, TX_INS, TX_OUTS, TIMESTAMP))
    Assert.assertNotEquals(TRANSACTION, Transaction(VERSION, TX_INS, TX_OUTS, 6))

    val WrongTxIn = Input("random", TX_OUT_INDEX, SCRIPTTXIN)
    val WrongListTxin = listOf(WrongTxIn)
    Assert.assertNotEquals(TRANSACTION, Transaction(VERSION, WrongListTxin, TX_OUTS, TIMESTAMP))

    val WrongTxOut = Output(4, SCRIPT_TX_OUT)
    val WrongListTxout = listOf(WrongTxOut)
    Assert.assertNotEquals(TRANSACTION, Transaction(VERSION, TX_INS, WrongListTxout, TIMESTAMP))
  }

  @Test
  fun computeIdTest() {
    val path = "protocol/examples/messageData/coin/post_transaction_coinbase.json"
    val validJson = JsonTestUtils.loadFile(path)

    val postTransactionModel = JsonTestUtils.parse(validJson) as PostTransactionCoin
    val transactionModel = postTransactionModel.transaction

    Assert.assertEquals(postTransactionModel.transactionId, transactionModel.computeId())
  }

  @Test
  fun verifySignature() {
    var path = "protocol/examples/messageData/coin/post_transaction_coinbase.json"
    var validJson = JsonTestUtils.loadFile(path)

    var postTransactionModel = JsonTestUtils.parse(validJson) as PostTransactionCoin
    var transactionModel = postTransactionModel.transaction
    var single = transactionModel.inputs[0]

    var sig = single.script.sig
    var pk = single.script.pubkey

    Assert.assertTrue(
      pk.verify(
        sig,
        Base64URLData(
          Transaction.computeSigOutputsPairTxOutHashAndIndex(
              transactionModel.outputs,
              Collections.singletonMap(single.txOutHash, single.txOutIndex)
            )
            .toByteArray(StandardCharsets.UTF_8)
        )
      )
    )

    path = "protocol/examples/messageData/coin/post_transaction_bad_signature.json"
    validJson = JsonTestUtils.loadFile(path)

    postTransactionModel = JsonTestUtils.parse(validJson) as PostTransactionCoin
    transactionModel = postTransactionModel.transaction
    single = transactionModel.inputs[0]
    sig = single.script.sig
    pk = single.script.pubkey

    Assert.assertFalse(
      pk.verify(
        sig,
        Base64URLData(
          Transaction.computeSigOutputsPairTxOutHashAndIndex(
              transactionModel.outputs,
              Collections.singletonMap(single.txOutHash, single.txOutIndex)
            )
            .toByteArray(StandardCharsets.UTF_8)
        )
      )
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
  }
}
