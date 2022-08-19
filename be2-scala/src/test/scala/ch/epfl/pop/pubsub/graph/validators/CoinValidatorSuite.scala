package ch.epfl.pop.pubsub.graph.validators

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.data.PostTransactionMessages._

import scala.reflect.io.Directory

class CoinValidatorSuite extends TestKit(ActorSystem("coinValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseCoinTest"

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  test("Posting a transaction works as intended") {
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a coinbase transaction works as intended") {
    val postTransaction = postTransactionCoinbase
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a large transaction works as intended") {
    val postTransaction = postTransactionMaxAmount
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a transaction with a zero amount succeeds") {
    val postTransaction = postTransactionZeroAmount
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a transaction with an incorrect ID does not work") {
    val postTransaction = postTransactionWrongTransactionId
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message shouldEqual Right(PipelineError(-4, "PostTransaction content validation failed: incorrect transaction id", Some(1)))
  }

  test("Posting a transaction with an incorrect signature does not work") {
    val postTransaction = postTransactionBadSignature
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message shouldEqual Right(PipelineError(-4, "PostTransaction content validation failed: bad signature", Some(1)))
  }

  test("Posting a transaction with an arithmetic overflow does not work") {
    val postTransaction = postTransactionOverflowSum
    val message: GraphMessage = CoinValidator.validatePostTransaction(postTransaction)
    message shouldEqual Right(PipelineError(-4, "PostTransaction content validation failed: uint53 addition overflow", Some(1)))
  }
}
