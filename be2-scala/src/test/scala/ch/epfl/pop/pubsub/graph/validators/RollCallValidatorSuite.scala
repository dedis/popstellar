package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.config.RuntimeEnvironment.deleteRecursively
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.Election.CastVoteElectionExamples._
import util.examples.Election.EndElectionExamples._
import util.examples.Election.OpenElectionExamples._
import util.examples.Election.SetupElectionExamples._
import util.examples.JsonRpcRequestExample._
import util.examples.RollCall.{CloseRollCallExamples, CreateRollCallExamples, OpenRollCallExamples}
import util.examples.RollCall.CreateRollCallExamples.{SENDER, _}

import java.io.File
import java.util.concurrent.TimeUnit

class RollCallValidatorSuite extends TestKit(ActorSystem("rollcallValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseRollCallTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new File(DB_TEST_FOLDER)
    deleteRecursively(directory)
  }

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(SENDER, List(SENDER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(SENDER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataWrong: ChannelData = ChannelData(ObjectType.INVALID, List.empty)
  private final val channelDataRight: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val rollcallDataCreate: RollCallData = RollCallData(CreateRollCallExamples.R_ID, ActionType.CREATE)
  private final val rollcallDataOpen: RollCallData = RollCallData(OpenRollCallExamples.UPDATE_ID, ActionType.OPEN)
  private final val rollcallDataClose: RollCallData = RollCallData(CloseRollCallExamples.UPDATE_ID, ActionType.CLOSE)

  private def mockDbWrongChannelCreate: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataCreate)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongToken: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataOpen)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannelOpen: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataOpen)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWorkingCreate: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataCreate)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWorkingOpen: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataOpen)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWorkingClose: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.ReadRollCallData(_) =>
          sender() ! DbActor.DbActorReadRollCallDataAck(rollcallDataClose)
      }
    })
    system.actorOf(dbActorMock)
  }

  // Create RollCall
  test("Create Roll Call works as intended") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_RPC)
    message should equal(Left(CREATE_ROLL_CALL_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Create Roll Call should fail with invalid Timestamp") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Create Roll Call should fail with invalid Timestamp order") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_WRONG_TIMESTAMP_ORDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Create Roll Call should fail with invalid id") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Create Roll Call should fail with wrong sender") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Create Roll Call should fail with wrong type of channel") {
    val dbActorRef = mockDbWrongChannelCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCreateRollCall(CREATE_ROLL_CALL_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Close RollCall
  test("Close Roll Call works as intended") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_RPC)
    message should equal(Left(CLOSE_ROLL_CALL_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with invalid Timestamp") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with invalid update id") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with duplicate attendees") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_DUPLICATE_ATTENDEES_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with wrong attendees") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_ATTENDEES_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with wrong sender") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail if it is already closed") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_ALREADY_CLOSED_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with wrong type of channel") {
    val dbActorRef = mockDbWrongChannelOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Close Roll Call should fail with wrong closes id") {
    val dbActorRef = mockDbWorkingOpen
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateCloseRollCall(CLOSE_ROLL_CALL_WRONG_CLOSES_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Open RollCall
  test("Open Roll Call works as intended") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_RPC)
    message should equal(Left(OPEN_ROLL_CALL_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should fail with invalid Timestamp") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should fail with invalid update id") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should fail with wrong sender") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should fail with wrong type of channel") {
    val dbActorRef = mockDbWrongChannelCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should fail with wrong opens id") {
    val dbActorRef = mockDbWorkingCreate
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_WRONG_OPENS_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open Roll Call should succeed with valid opens id after closing a roll call") {
    val dbActorRef = mockDbWorkingClose
    val message: GraphMessage = new RollCallValidator(dbActorRef).validateOpenRollCall(OPEN_ROLL_CALL_VALID_OPENS_RPC)
    message should equal(Left(OPEN_ROLL_CALL_VALID_OPENS_RPC))
    system.stop(dbActorRef.actorRef)
  }
}
