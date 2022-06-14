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
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
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
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRightSetup: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrongSetup: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)

  private final val channelDataRightElection: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)
  private final val channelDataWrongElection: ChannelData = ChannelData(ObjectType.LAO, List.empty)


  private def mockDbWorkingSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongSetupBadChannel: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
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
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  // Valid meeting setup
  test("Creating a valid meeting works as intended") {
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_RPC)
    message should equal(Left(CREATE_MEETING_RPC))
    system.stop(dbActorRef.actorRef)
  }

  // Invalid meeting setups

  // invalid channel
  test("Creating an invalid meeting with invalid channel") {
    val dbActorRef = mockDbWrongSetupBadChannel
    println(dbActorRef)
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

   test("Creating an invalid meeting with invalid sender") {
    val dbActorRef = mockDbInvalidSetupWrongOwner
    println(dbActorRef)
    val message: GraphMessage = new MeetingValidator(dbActorRef).validateCreateMeeting(CREATE_MEETING_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }


}