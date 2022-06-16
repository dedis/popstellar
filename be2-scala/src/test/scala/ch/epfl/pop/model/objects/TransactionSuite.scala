package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import util.examples.data.PostTransactionMessages
import util.examples.data.TestKeyPairs._
import ch.epfl.pop.model.objects.Transaction
import scala.collection.immutable.SortedMap
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction

class TransactionSuite extends FunSuite with Matchers {
  test("transaction_id computes the correct hash") {
    val transaction = PostTransactionMessages.postTransaction.getDecodedData.get.asInstanceOf[PostTransaction].transaction

    val strings = {
      def rec(node: Any): List[String] = node match {
        case s: String => List(s)
        case data: Base64Data => List(data.toString)
        case n: Int => List(n.toString)
        case n: Long => List(n.toString)
        case node: Product =>
          val elements = node.productElementNames zip node.productIterator to SortedMap
          elements.values.toList.flatMap { rec }
      }
      rec(transaction)
    }
    val expected = Hash.fromStrings(strings: _*)

    transaction.transactionId shouldEqual expected
  }

  test("checkSignature accepts a valid signature") {
    val transaction = PostTransactionMessages.postTransaction.getDecodedData.get.asInstanceOf[PostTransaction].transaction
    transaction.checkSignatures() shouldEqual true
  }

  test("checkSignature reject an invalid signature") {
    val transaction = PostTransactionMessages.postTransactionBadSignature.getDecodedData.get.asInstanceOf[PostTransaction].transaction
    transaction.checkSignatures() shouldEqual false
  }

  test("sign produces a valid signature") {
    val transaction = PostTransactionMessages.postTransactionBadSignature.getDecodedData.get.asInstanceOf[PostTransaction].transaction
    val signedTransaction = transaction.sign(Seq(keypairs(1).keyPair))
    signedTransaction.checkSignatures() shouldEqual true
  }

  test("signature is performed over the correct data") {
    val transaction = PostTransactionMessages.postTransaction.getDecodedData.get.asInstanceOf[PostTransaction].transaction
    val signedTransaction = transaction.sign(Seq(keypairs(1).keyPair))
    val signaturePayload = "01N1Y8twdu7wpdz5HLnkIeQSeuKpkNcQHeKF7XabLYU=" + "0" + "32" + "P2PKH" + "-_qR4IHwsiq50raa8jURNArds54="
    Signature(signedTransaction.inputs(0).script.sig).verify(keypairs(1).keyPair.publicKey, Base64Data.encode(signaturePayload))
  }
}
