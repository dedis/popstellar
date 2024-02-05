package com.github.dedis.popstellar.model.objects

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.security.GeneralSecurityException
import java.util.Collections
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionObjectTest {
  private var senderKey: KeyPair = Base64DataUtils.generateKeyPair()
  var sender = senderKey.publicKey

  @Throws(GeneralSecurityException::class)
  @Test
  fun channelTest() {
    val channel = fromString("/root/laoId/coin/myChannel")
    val builder = validTransactionBuilder
    builder.setChannel(channel)
    Assert.assertEquals(channel, builder.build().channel)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun inputsTest() {
    // test get Inputs
    val builder = validTransactionBuilder
    val txOutIndex = 0
    val txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    val type = "P2PKH"
    val scriptTxIn: ScriptInputObject
    val sig = senderKey.sign(sender).encoded
    scriptTxIn = ScriptInputObject(type, sender, Signature(sig))
    val input = InputObject(txOutHash, txOutIndex, scriptTxIn)
    val listInput = listOf(input)
    builder.setInputs(listInput)

    Assert.assertEquals(listInput, builder.build().inputs)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun outputsTest() {
    // test get Outputs
    val builder = validTransactionBuilder
    val type = "P2PKH"
    val pubKeyHash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    builder.setOutputs(listOutput)

    Assert.assertEquals(listOutput, builder.build().outputs)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun lockTimeTest() {
    val builder = validTransactionBuilder
    val locktime: Long = 0
    builder.setLockTime(locktime)
    Assert.assertEquals(locktime, builder.build().lockTime)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun versionTest() {
    val builder = validTransactionBuilder
    val version = 0
    builder.setVersion(version)
    Assert.assertEquals(version.toLong(), builder.build().version.toLong())
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun sendersTransactionTest() {
    val builder = validTransactionBuilder
    val txOutIndex = 0
    val txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    val type = "P2PKH"
    val pubKey = sender.encoded
    val scriptTxIn: ScriptInputObject
    val sig = "dhfqkdfhqu"
    scriptTxIn = ScriptInputObject(type, PublicKey(pubKey), Signature(sig))
    val input = InputObject(txOutHash, txOutIndex, scriptTxIn)
    val listInput = listOf(input)
    builder.setInputs(listInput)

    Assert.assertEquals(listOf(sender), builder.build().sendersTransaction)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun receiversHashTransactionTest() {
    val builder = validTransactionBuilder
    val type = "P2PKH"
    val pubKeyHash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    builder.setOutputs(listOutput)

    Assert.assertEquals(listOf(pubKeyHash), builder.build().receiversHashTransaction)
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun receiversTransactionTestNull() {
    // test thrown null List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey)
    val builder = validTransactionBuilder
    val senderKey1 = Base64DataUtils.generateKeyPair()
    val sender1 = senderKey1.publicKey
    val sender2: PublicKey? = null
    val type = "P2PKH"
    val pubkeyhash1 = sender1.computeHash()
    val pubkeyhash2 = "none"
    val scriptTxOut = ScriptOutputObject(type, pubkeyhash1)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    val mapHash = Collections.singletonMap(pubkeyhash2, sender2)
    builder.setOutputs(listOutput)
    val transactionObject = builder.build()
    Assert.assertThrows(IllegalArgumentException::class.java) {
      transactionObject.getReceiversTransaction(mapHash.toMap())
    }
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun receiversTransactionTest() {
    val builder = validTransactionBuilder
    val type = "P2PKH"
    val pubkeyhash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubkeyhash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    val mapHash = Collections.singletonMap(pubkeyhash, sender)
    builder.setOutputs(listOutput)
    Assert.assertEquals(listOf(sender), builder.build().getReceiversTransaction(mapHash))
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun isReceiverTest() {
    val builder = validTransactionBuilder
    val type = "P2PKH"
    val pubKeyHash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    builder.setOutputs(listOutput)
    val transactionObject = builder.build()
    Assert.assertEquals(listOf(pubKeyHash), transactionObject.receiversHashTransaction)
    // DUMMY SENDER
    val senderDummyKey = Base64DataUtils.generateKeyPair()
    val senderDummy = senderDummyKey.publicKey
    Assert.assertNotEquals(
      listOf(senderDummy.computeHash()),
      transactionObject.receiversHashTransaction
    )
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun isSenderTest() {
    // test boolean is Sender(PublicKey publicKey)
    val builder = validTransactionBuilder
    val type = "P2PKH"
    val scriptTxInput = ScriptInputObject(type, sender, Signature("qqchose"))
    val input = InputObject(hash("none"), 0, scriptTxInput)
    val listInput = listOf(input)
    builder.setInputs(listInput)
    Assert.assertTrue(builder.build().isSender(sender))
  }

  @Throws(GeneralSecurityException::class)
  @Test
  fun indexTransactionTest() {
    // test int get_index_transaction(PublicKey publicKey)
    val builder = validTransactionBuilder
    val type = "P2PKH"
    // RECEIVER
    val pubKeyHash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)
    builder.setOutputs(listOutput)
    Assert.assertEquals(0, builder.build().getIndexTransaction(sender).toLong())
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun computeIdTest() {
    val path = "protocol/examples/messageData/coin/post_transaction_coinbase.json"
    val validJson = loadFile(path)
    val postTransactionModel = parse(validJson) as PostTransactionCoin
    val transactionModel = postTransactionModel.transaction
    val builder = validTransactionBuilder
    builder.setLockTime(transactionModel.lockTime)
    builder.setVersion(transactionModel.version)
    val inpObj: MutableList<InputObject> = ArrayList()
    val outObj: MutableList<OutputObject> = ArrayList()
    for (i in transactionModel.inputs) {
      val scriptInput = i.script
      inpObj.add(
        InputObject(
          i.txOutHash,
          i.txOutIndex,
          ScriptInputObject(scriptInput.type, scriptInput.pubkey, scriptInput.sig)
        )
      )
    }
    for (o in transactionModel.outputs) {
      val scriptOutput = o.script
      outObj.add(
        OutputObject(o.value, ScriptOutputObject(scriptOutput.type, scriptOutput.pubKeyHash))
      )
    }
    builder.setInputs(inpObj)
    builder.setOutputs(outObj)
    val transactionObject = builder.build()
    Assert.assertTrue(transactionObject.isCoinBaseTransaction)
  }

  @get:Throws(GeneralSecurityException::class)
  private val validTransactionBuilder: TransactionObjectBuilder
    get() = getValidTransactionBuilder("a")

  @Throws(GeneralSecurityException::class)
  private fun getValidTransactionBuilder(transactionId: String): TransactionObjectBuilder {
    val builder = TransactionObjectBuilder()

    val type = "P2PKH"
    val txOutIndex = 0
    val txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    val scriptTxIn: ScriptInputObject
    val sig = senderKey.sign(sender).encoded
    scriptTxIn = ScriptInputObject(type, sender, Signature(sig))
    val input = InputObject(txOutHash, txOutIndex, scriptTxIn)
    val listInput = listOf(input)
    builder.setInputs(listInput)

    val channel = fromString("/root/laoId/coin/myChannel")
    builder.setChannel(channel)

    val pubKeyHash = sender.computeHash()
    val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
    val value = 32
    val output = OutputObject(value.toLong(), scriptTxOut)
    val listOutput = listOf(output)

    builder.setOutputs(listOutput)
    builder.setLockTime(0)
    builder.setVersion(0)
    builder.setTransactionId(transactionId)

    return builder
  }
}
