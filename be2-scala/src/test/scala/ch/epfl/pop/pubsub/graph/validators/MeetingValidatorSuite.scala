package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
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
import util.examples.Election.SetupElectionExamples
import util.examples.JsonRpcRequestExample._

import java.io.File
import java.util.concurrent.TimeUnit
import scala.reflect.io.Directory

class MertingValidatorSuite extends TestKit(ActorSystem("meetingValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseMeetingTest"

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

  private final val sender: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataLeft: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataLeftSetup: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrongSetup: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)

  private final val channelDataLeftElection: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)
  private final val channelDataWrongElection: ChannelData = ChannelData(ObjectType.LAO, List.empty)

  private def mockDbWorkingSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataLeft)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataLeftSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongSetupBadChannel: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataLeft)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrongSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbInvalidSetupWrongOwner: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataLeftSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  // Valid meeting setup
  test("Creating a valid meeting works as intended") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_RPC)
    message should equal(Right(CREATE_MEETING_RPC))
    system.stop(dbActorRef.actorRef)
  }

  // Invalid meeting setups

  // invalid channel
  test("Creating an invalid meeting with invalid channel") {
    val dbActorRef = mockDbWrongSetupBadChannel
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_WRONG_CHANNEL_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_WRONG_CHANNEL_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // invalid sender
  test("Creating an invalid meeting with invalid sender") {
    val dbActorRef = mockDbInvalidSetupWrongOwner
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
  // invalid data hash
  test("Creating an invalid meeting with invalid data hash") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_INVALID_DATA_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_INVALID_DATA_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // invalid creation time
  test("Creating an invalid meeting with invalid creation time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_INVALID_CREATION_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_INVALID_CREATION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // invalid start time
  test("Creating an invalid meeting with invalid start time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_INVALID_START_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_INVALID_START_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // invalid start and end time
  test("Creating an invalid meeting with start time > end time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_INVALID_STARTEND_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_INVALID_STARTEND_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // invalid end time
  test("Creating an invalid meeting with invalid end time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_INVALID_END_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateCreateMeeting(CREATE_MEETING_INVALID_END_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Valid meeting state
  test("Creating a valid meeting state works as intended") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_RPC)
    message should equal(Right(STATE_MEETING_RPC))
    system.stop(dbActorRef.actorRef)
  }

  // Invalid meeting states

  // Invalid data hash
  test("Creating and invalid meeting state with invalid data") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_INVALID_DATA_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_INVALID_DATA_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Stale creation time
  test("Creating and invalid meeting state with stale creation time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_INVALID_CREATION_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_INVALID_CREATION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Stale start time
  test("Creating and invalid meeting state with stale start time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_INVALID_START_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_INVALID_START_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Creation time > End Time
  test("Creating and invalid meeting state with end time < creation time") {
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_SMALL_END_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_SMALL_END_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Start time > End Time
  test("Creating and invalid meeting state with start time > end time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_BIG_START_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_BIG_START_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Wrong Witness Signature
  test("Creating and invalid meeting state with wrong witness signature") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_WRONGWITNESS_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_WRONGWITNESS_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Last modified time > creation time
  test("Creating and invalid meeting state with small modification time") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateStateMeeting(STATE_MEETING_SMALLMODIFICATION_RPC)
    val messageStandardActor: GraphMessage = MeetingValidator.validateStateMeeting(STATE_MEETING_SMALLMODIFICATION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
}
