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
  private final val attendee1: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHaKYsa0obpotjoc-wwtkeKods9WBcY="))
  private final val attendee2: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHcKYsa0obpotjoc-wwtkeKods9WBcY="))
  private final val attendee3: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHdKYsa0obpotjoc-wwtkeKods9WBcY="))

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_WRONG: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataForResultElection: LaoData = LaoData(sender, List(attendee1, attendee2, attendee3), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_WRONG, List(PK_WRONG), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRightSetup: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrongSetup: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)

  private final val channelDataRightElection: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)
  private final val channelDataWrongElection: ChannelData = ChannelData(ObjectType.LAO, List.empty)

  private final val channelDataWithSetupAndOpenAndCastMessage: ChannelData = ChannelData(ObjectType.ELECTION, List(DATA_CAST_VOTE_MESSAGE, DATA_SET_UP_OPEN_BALLOT, DATA_OPEN_MESSAGE))
  private final val channelDataWrongChannelCastVote: ChannelData = ChannelData(ObjectType.LAO, List(DATA_CAST_VOTE_MESSAGE, DATA_SET_UP_OPEN_BALLOT, DATA_OPEN_MESSAGE))
  private final val channelDataWithSetupAndCastMessage: ChannelData = ChannelData(ObjectType.ELECTION, List(DATA_CAST_VOTE_MESSAGE, DATA_SET_UP_OPEN_BALLOT))
  private final val channelDataWithEndElectionMessage: ChannelData = ChannelData(ObjectType.ELECTION, List(DATA_CAST_VOTE_MESSAGE, DATA_SET_UP_OPEN_BALLOT, DATA_OPEN_MESSAGE, DATA_END_ELECTION_MESSAGE))
  private final val messagesNotEnd: List[Message] = List(MESSAGE_CAST_VOTE_ELECTION_WORKING, MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING, MESSAGE_OPEN_ELECTION_WORKING)
  private final val messages: List[Message] = MESSAGE_END_ELECTION_WORKING :: messagesNotEnd

  private val keyPair: KeyPair = KeyPair()
  private val electionData: ElectionData = ElectionData(Hash(Base64Data.encode("election")), keyPair)

  private def mockDbWorkingSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightSetup)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))

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
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndOpenAndCastMessage)
        case DbActor.ReadElectionData(_, _) =>
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))
        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messagesNotEnd)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))

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
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndOpenAndCastMessage)
        case DbActor.ReadElectionData(_, _) =>
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))
        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
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
        case DbActor.ReadElectionData(_, _) =>
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))
        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbMissingOpenElectionMessage: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndCastMessage)
        case DbActor.ReadElectionData(_, _) =>
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))
        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWithEndElectionMessage: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithEndElectionMessage)
        case DbActor.ReadElectionData(_, _) =>
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))
        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))
        case DbActor.Read(_, DATA_END_ELECTION_MESSAGE) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_END_ELECTION_WORKING))
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbForResultElection: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataForResultElection)
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(List(MESSAGE_END_ELECTION_WORKING))
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbElectionNotEnded: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataForResultElection)
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(List.empty)
        case DbActor.ReadSetupElectionMessage(_) =>
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))
      }
    })
    system.actorOf(dbActorMock)
  }

  // Setup Election
  test("Setting up an election works as intended") {
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    message should equal(Right(SETUP_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_RPC)
    val messageStandardActor = ElectionValidator.validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp order between start and end fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid Timestamp order between created_at and start fails") {
    val dbActorRef = mockDbWorkingSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC2)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC2)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannelSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_ID_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with invalid question id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_QUESTION_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(SETUP_ELECTION_WRONG_OWNER_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(SETUP_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Setting up an election without Params does not work in validateSetupElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateSetupElection(RPC_NO_PARAMS)
    val messageStandardActor: GraphMessage = ElectionValidator.validateSetupElection(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Key Election
  test("KeyElection works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(KEY_ELECTION_RPC)
    message should equal(Right(KEY_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("KeyElection fails with invalid election id") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(KEY_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("KeyElection fails with wrong sender") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(KEY_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("KeyElection fails with invalid PoP token") {
    val dbActorRef = mockDbWrongTokenSetup
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(KEY_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("KeyElection fails with wrong channel") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(KEY_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("KeyElection without Params does not work in validateKeyElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateKeyElection(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // Open Election
  test("Open up an election works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    message should equal(Right(OPEN_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid Timestamp order between the open Election and the setup Election fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_BEFORE_SETUP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(OPEN_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(OPEN_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_ID_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(OPEN_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with invalid lao id fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_LAO_ID_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(OPEN_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election with wrong sender fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(OPEN_ELECTION_WRONG_OWNER_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(OPEN_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Open up an election without Params does not work in validateOpenElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateOpenElection(RPC_NO_PARAMS)
    val messageStandardActor: GraphMessage = ElectionValidator.validateOpenElection(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    messageStandardActor shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // CastVote Election
  test("Casting a vote works as intended") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message should equal(Right(CAST_VOTE_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid Timestamp fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannelCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with invalid lao id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote with wrong sender fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote without Params does not work in validateCastVoteElection") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting an invalid vote should fail") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_INVALID_VOTE_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting an invalid ballot should fail") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_INVALID_BALLOT_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting an invalid vote id should fail") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_INVALID_VOTE_ID_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote if election is not open should fail") {
    val dbActorRef = mockDbMissingOpenElectionMessage
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote in an election that was ended should fail") {
    val dbActorRef = mockDbWithEndElectionMessage
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_ELECTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Casting a vote before opening the election should fail") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateCastVoteElection(CAST_VOTE_BEFORE_OPENING_THE_ELECTION)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // End Election
  test("Ending an election works as intended") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    system.log.info("Responding with a Ack")
    message should equal(Right(END_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid Timestamp fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_TIMESTAMP_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannelCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_ID_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_WRONG_ID_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with invalid lao id fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_LAO_ID_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_WRONG_LAO_ID_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election with wrong sender fails") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_WRONG_OWNER_RPC)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(END_ELECTION_WRONG_OWNER_RPC)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election without Params does not work in validateEndElection") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(RPC_NO_PARAMS)
    val messageStandardActor: GraphMessage = ElectionValidator.validateEndElection(RPC_NO_PARAMS)
    message shouldBe a[Left[PipelineError, _]]
    messageStandardActor shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Ending an election before the setup does not work in validateEndElection") {
    val dbActorRef = mockDbCastVote
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateEndElection(END_ELECTION_BEFORE_SETUP_RPC)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("Receiving the result of an election works as intended") {
    val dbActorRef: AskableActorRef = mockDbForResultElection
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC)
    message should equal(Right(RESULT_ELECTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("ResultElection with negative number of votes does not work in validateResultElection") {
    val dbActorRef: AskableActorRef = mockDbForResultElection
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC_WRONG)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("ResultElection with number of votes exceeding the number of attendees does not work in validateResultElection") {
    val dbActorRef: AskableActorRef = mockDbForResultElection
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC_TOO_MUCH_VOTES)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("ResultElection with election not ended does not work in validateResultElection ") {
    val dbActorRef: AskableActorRef = mockDbElectionNotEnded
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("ResultElection with invalid question ids does not work in validateResultElection ") {
    val dbActorRef: AskableActorRef = mockDbForResultElection
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC_WRONG_ID)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }

  test("ResultElection with incoherent ballot options does not work in validateResultElection") {
    val dbActorRef: AskableActorRef = mockDbForResultElection
    val message: GraphMessage = new ElectionValidator(dbActorRef).validateResultElection(RESULT_ELECTION_RPC_WRONG_BALLOT_OPTIONS)
    message shouldBe a[Left[PipelineError, _]]
    system.stop(dbActorRef.actorRef)
  }
}
