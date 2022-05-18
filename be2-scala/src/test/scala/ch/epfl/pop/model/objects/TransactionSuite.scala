package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import util.examples.data.PostTransactionMessages
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
}
