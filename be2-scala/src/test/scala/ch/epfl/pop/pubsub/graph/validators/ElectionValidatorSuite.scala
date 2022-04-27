package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.Election.{CastVoteElectionExamples, SetupElectionExamples}
import util.examples.JsonRpcRequestExample._

import java.io.File
import java.util.concurrent.TimeUnit
import scala.reflect.io.Directory

class ElectionValidatorSuite extends TestKit(ActorSystem("electionValidatorTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseElectionTest"

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

  private final val sender: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRightSetup: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrongSetup: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)

  private final val channelDataRightElection: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)
  private final val channelDataWrongElection: ChannelData =ChannelData(ObjectType.LAO, List.empty)

  private final val channelDataWithSetupAndOpenMessage: ChannelData = CastVoteElectionExamples.CHANNEL_DATA
  private final val channelDataWrongChannelCastVote: ChannelData = CastVoteElectionExamples.WRONG_CHANNEL_DATA


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

  private def mockDbWrongTokenSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannelSetup: AskableActorRef = {
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

  private def mockDbWorking: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightElection)
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
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightElection)
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
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrongElection)
      }
    })
    system.actorOf(dbActorMock)
  }


  private def mockDbCastVote: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndOpenMessage)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongTokenCastVote: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndOpenMessage)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannelCastVote: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrongChannelCastVote)
      }
    })
    system.actorOf(dbActorMock)
  }

  //Setup Election
  test("Setting up an election works as intended") {
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    message should equal(Left(SETUP_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp order between start and end fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp order between created_at and start fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC2)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannelSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election without Params does not work in validateSetupElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  //Open Election
  test("Open up an election works as intended") {
    val dbActorRef = mockDbWorking
    println(dbActorRef)
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    message should equal(Left(OPEN_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid lao id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election without Params does not work in validateOpenElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  //CastVote Election
  test("Casting a vote works as intended") {
    val dbActorRef = mockDbCastVote
    println(dbActorRef)
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message should equal(Left(CAST_VOTE_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid Timestamp fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannelCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid lao id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with wrong sender fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote without Params does not work in validateCastVoteElection") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  //End Election
  test("Ending an election works as intended") {
    val dbActorRef = mockDbWorking
    println(dbActorRef)
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    message should equal(Left(END_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid lao id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election without Params does not work in validateEndElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }
}
