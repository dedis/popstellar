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
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.data.PostTransactionMessages._

import scala.reflect.io.Directory

class CashValidatorSuite extends TestKit(ActorSystem("cashValidatorTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseCashTest"

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
    val message: GraphMessage = CashValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a large transaction works as intended") {
    val postTransaction = postTransactionMaxAmount
    val message: GraphMessage = CashValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

  test("Posting a transaction with a zero amount succeeds") {
    val postTransaction = postTransactionZeroAmount
    val message: GraphMessage = CashValidator.validatePostTransaction(postTransaction)
    message should equal(Left(postTransaction))
  }

}
