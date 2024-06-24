package ch.epfl.pop.pubsub.graph.validators

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike as FunSuiteLike
import org.scalatest.matchers.should.Matchers
import util.examples.Federation.FederationExpectExample.{CHALLENGE, EXPECT_MESSAGE}
import util.examples.Federation.FederationInitExample.INIT_MESSAGE
import util.examples.JsonRpcRequestExample.*

import java.io.File
import java.util.concurrent.TimeUnit
import scala.reflect.io.Directory

class FederationValidatorSuite extends TestKit(ActorSystem("FederationValidatorTestActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseFederationTest"

  // TODO:maybe to remove if not used
  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  override def afterAll(): Unit = {
    // Stops the testkit
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  private final val SENDER: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val ADDRESS: String = "127.0.0.1:8000"

  private final val laoDataRight: LaoData = LaoData(SENDER, List(SENDER), PRIVATE_KEY, PUBLIC_KEY, List.empty, ADDRESS)
  private final val channelDataRight: ChannelData = ChannelData(ObjectType.federation, List.empty)
  private final val channelDataWrong: ChannelData = ChannelData(ObjectType.lao, List.empty)

  private def mockDbWorking: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.ReadFederationChallenge(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(CHALLENGE))
        case DbActor.ReadFederationExpect(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(EXPECT_MESSAGE))
        case DbActor.ReadFederationInit(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(INIT_MESSAGE))
      }

    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannel: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
        case DbActor.ReadFederationChallenge(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(CHALLENGE))
        case DbActor.ReadFederationExpect(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(EXPECT_MESSAGE))
        case DbActor.ReadFederationInit(_,_) =>
          sender() ! DbActor.DbActorReadAck(Some(INIT_MESSAGE))
      }
    })
    system.actorOf(dbActorMock)
  }

  // FederationChallengeRequest
  test("FederationChallengeRequest works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallengeRequest(CHALLENGE_REQUEST_RPC)
    message should equal(Right(CHALLENGE_REQUEST_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("FederationChallengeRequest with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallengeRequest(CHALLENGE_REQUEST_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
  test("FederationChallengeRequest on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallengeRequest(CHALLENGE_REQUEST_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
  test("FederationChallengeRequest with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallengeRequest(CHALLENGE_REQUEST_WRONG_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating an RpcMessage without Params does not work in validateFederationChallengeRequest") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallengeRequest(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // FederationExpect
  test("FederationExpect works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_RPC)
    message should equal(Right(EXPECT_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("FederationExpect with invalid server address fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_WRONG_SERVER_ADDRESS_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationExpect with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_WRONG_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationExpect with wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationExpect with invalid challenge fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_WRONG_CHALLENGE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationExpect with wrong challenge sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(EXPECT_WRONG_CHALLENGE_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating an RpcMessage without Params does not work in validateFederationExpect") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationExpect(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // FederationInit
  test("FederationInit works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_RPC)
    message should equal(Right(INIT_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("FederationInit with invalid server address fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_WRONG_SERVER_ADDRESS_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationInit with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_WRONG_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationInit with wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationInit with invalid challenge fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_WRONG_CHALLENGE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationInit with wrong challenge sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(INIT_WRONG_CHALLENGE_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating an RpcMessage without Params does not work in validateFederationInit") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationInit(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // FederationChallenge
  test("FederatinoChallenge works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(CHALLENGE_RPC)
    message should equal(Right(CHALLENGE_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Federation challenge with stale timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(CHALLENGE_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationChallenge with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(CHALLENGE_WRONG_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationChallenge with wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(CHALLENGE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationChallenge with invalid challenge value fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(CHALLENGE_WRONG_VALUE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating an RpcMessage without Params does not work in validateFederationChallenge") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationChallenge(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // FederationResult
  test("FederationResult works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_RPC)
    message should equal(Right(RESULT_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("FederationResult with invalid status fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_WRONG_STATUS_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationResult with wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationResult with invalid challenge fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_WRONG_CHALLENGE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationResult fails if the challenge sender is not the organizer") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_WRONG_CHALLENGE_SENDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("FederationResult fails if the publicKey field is not the sender of FederationInit") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RESULT_WRONG_PUBLIC_KEY_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating an RpcMessage without Params does not work in validateFederationResult") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new FederationValidator(dbActorRef).validateFederationResult(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

}
