package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Base64Data, ChannelData, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}

//import util.examples.MessageExample._
import java.io.File
import java.util.concurrent.TimeUnit

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample._

import scala.reflect.io.Directory

class LaoValidatorSuite extends TestKit(ActorSystem("laoValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  final val DB_TEST_FOLDER: String = "databaseLaoTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  test("LAO creation works as intended") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_RPC)
    message should equal(Left(CREATE_LAO_RPC))
  }

  test("LAO creation fails with wrong channel") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with stale Timestamp") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with duplicate witnesses") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_WITNESSES_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong id") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong sender") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with empty name") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_EMPTY_NAME_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails without ParamsWithMessage") {
    val message: GraphMessage = LaoValidator.validateCreateLao(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
  }

  // GreetLao tests
  test("LAO greeting works as intended") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_RPC)
    message should equal(Left(GREET_LAO_RPC))
  }

  test("LAO greeting fails with wrong lao id") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_LAO_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong frontend") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_FRONTEND_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong address") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_ADDRESS_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong sender") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong channel") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails without ParamsWithMessage") {
    val message: GraphMessage = LaoValidator.validateCreateLao(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
  }
}
