package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.Election.CastVoteElectionExamples._
import util.examples.Election.EndElectionExamples._
import util.examples.Election.OpenElectionExamples._
import util.examples.Election.SetupElectionExamples._
import util.examples.Election._
import util.examples.data._
import util.examples.LaoDataExample

import scala.concurrent.duration.FiniteDuration

class ElectionHandlerTest extends TestKit(ActorSystem("Election-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  private val keyPair: KeyPair = KeyPair()
  private val electionData: ElectionData = ElectionData(Hash(Base64Data.encode("election")), keyPair)

  private final val sender: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)

  private final val channelDataWithSetupAndOpenAndCastMessage: ChannelData = ChannelData(ObjectType.ELECTION, List(DATA_CAST_VOTE_MESSAGE, DATA_SET_UP_OPEN_BALLOT, DATA_OPEN_MESSAGE))
  private final val messages: List[Message] = List(MESSAGE_CAST_VOTE_ELECTION_WORKING, MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING, MESSAGE_OPEN_ELECTION_WORKING, MESSAGE_END_ELECTION_WORKING)

  def mockDbWithNack: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) =>
          system.log.info(f"Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _)
            | DbActor.ChannelExists(_)
            | DbActor.CreateChannel(_, _)
            | DbActor.CreateElectionData(_, _, _)
            | DbActor.WriteSetupElectionMessage(_, _) =>
          system.log.info(f"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ReadLaoData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
        case DbActor.ReadElectionData(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbElectionNotSetUp: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _)
            | DbActor.CreateChannel(_, _)
            | DbActor.CreateElectionData(_, _, _)
            | DbActor.WriteSetupElectionMessage(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ChannelExists(_) =>
          system.log.info("Received a create setup message")
          system.log.info("Responding with a no")
          sender() ! Status.Failure(DbActorNAckException(1, "no"))
        case DbActor.ReadLaoData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
        case DbActor.ReadElectionData(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbElectionSecretBallotReadFailed: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _)
            | DbActor.CreateChannel(_, _)
            | DbActor.CreateElectionData(_, _, _)
            | DbActor.WriteSetupElectionMessage(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ChannelExists(_) =>
          system.log.info("Received a channel exist setup message")
          system.log.info("Responding with a no")
          sender() ! Status.Failure(DbActorNAckException(1, "no"))
        case DbActor.ReadLaoData(_) | DbActor.ReadElectionData(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! Status.Failure(DbActorNAckException(1, "no"))
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAckEndElection: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()

        case DbActor.ReadLaoData(_) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)

        case DbActor.ReadElectionData(_, _) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)

        case DbActor.ReadChannelData(_) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWithSetupAndOpenAndCastMessage)

        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))

        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))

        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))

        case DbActor.Catchup(_) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorCatchupAck(messages)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithNAckEndElection: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ReadLaoData(_) | DbActor.ReadChannelData(_) =>
          system.log.info(f"Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))

        case DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) =>
          system.log.info("Responding with an Ack")
          sender() ! DbActor.DbActorAck()

        case DbActor.Read(_, DATA_CAST_VOTE_MESSAGE) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_CAST_VOTE_ELECTION_WORKING))

        case DbActor.Read(_, DATA_SET_UP_OPEN_BALLOT) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING))

        case DbActor.Read(_, DATA_OPEN_MESSAGE) =>
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadAck(Some(MESSAGE_OPEN_ELECTION_WORKING))

        case DbActor.Catchup(_) =>
          system.log.info("Responding with a Ack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
      }
    })
    system.actorOf(dbActorMock)
  }

  test("SetupElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElection

    rc.handleSetupElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection should succeed if the election doesn't already exists in database") {
    val mockedDB = mockDbElectionNotSetUp
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElection

    rc.handleSetupElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection with secret ballot should succeed if it is stored correctly in the database") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElectionSecretBallot

    rc.handleSetupElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection with secret ballot should fail if the election cannot read in the lao data") {
    val mockedDB = mockDbElectionSecretBallotReadFailed
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElectionSecretBallot

    rc.handleSetupElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should fail if the election does not exist") {
    val mockedDB = mockDbElectionNotSetUp
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("CastVoteElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = CastVoteElectionMessages.castVoteElection

    rc.handleCastVoteElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("CastVoteElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = CastVoteElectionMessages.castVoteElection

    rc.handleCastVoteElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  /*test("EndElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAckEndElection
    val rc = new ElectionHandler(mockedDB)
    val request = EndElectionMessages.endElection

    rc.handleEndElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }*/

  test("EndElection should fail if the election does not exist") {
    val mockedDB = mockDbElectionNotSetUp
    val rc = new ElectionHandler(mockedDB)
    val request = EndElectionMessages.endElection

    rc.handleOpenElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("EndElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNAckEndElection
    val rc = new ElectionHandler(mockedDB)
    val request = EndElectionMessages.endElection

    rc.handleEndElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("KeyElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = KeyElectionMessages.keyElection

    rc.handleKeyElection(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("keyElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = KeyElectionMessages.keyElection

    rc.handleKeyElection(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }
}
