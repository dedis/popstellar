package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.JsonRpcRequestExample._
import util.examples.Witness.WitnessMessageExamples

import java.io.File
import java.util.concurrent.TimeUnit
import scala.reflect.io.Directory

class WitnessValidatorSuite extends TestKit(ActorSystem("witnessValidatorTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseWitnessTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  private final val PUBLIC_KEY: PublicKey = WitnessMessageExamples.SENDER
  private final val PRIVATE_KEY: PrivateKey = WitnessMessageExamples.privateKey
  private final val PK_WRONG_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(PUBLIC_KEY, List(PUBLIC_KEY), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_WRONG_OWNER, List(PK_WRONG_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)

  private def mockDbWorking: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongToken: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  //WitnessMessage
  test("Witnessing a message works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new WitnessValidator(dbActorRef).validateWitnessMessage(WITNESS_MESSAGE_RPC)
    val standardActorMessage: GraphMessage = WitnessValidator.validateWitnessMessage(WITNESS_MESSAGE_RPC)
    message should equal(Left(WITNESS_MESSAGE_RPC))
    standardActorMessage should equal(Left(WITNESS_MESSAGE_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Witnessing a message without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new WitnessValidator(dbActorRef).validateWitnessMessage(WITNESS_MESSAGE_RPC)
    val standardActorMessage: GraphMessage = WitnessValidator.validateWitnessMessage(WITNESS_MESSAGE_RPC)
    message shouldBe a[Right[_, PipelineError]]
    standardActorMessage shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Witnessing a message with wrong signature fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new WitnessValidator(dbActorRef).validateWitnessMessage(WITNESS_MESSAGE_WRONG_SIGNATURE_RPC)
    val standardActorMessage: GraphMessage = WitnessValidator.validateWitnessMessage(WITNESS_MESSAGE_WRONG_SIGNATURE_RPC)
    message shouldBe a[Right[_, PipelineError]]
    standardActorMessage shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating a RpcMessage without Params does not work in validateWitnessMessage") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new WitnessValidator(dbActorRef).validateWitnessMessage(RPC_NO_PARAMS)
    val standardActorMessage: GraphMessage = WitnessValidator.validateWitnessMessage(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
    standardActorMessage shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
}
