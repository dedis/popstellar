package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.{Base64Data, DbActorNAckException, ElectionData, Hash, KeyPair, PrivateKey}
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.LaoDataExample
import util.examples.data.{KeyElectionMessages, OpenElectionMessages, SetupElectionMessages}

import java.util.Base64
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

  def mockDbWithNack: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) | DbActor.CreateElectionData(_, _)=>
          system.log.info(f"Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-NACK")
  }

  def mockDbWithAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) | DbActor.CreateElectionData(_, _)=>
          system.log.info(f"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ReadLaoData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
        case DbActor.ReadElectionData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
      }
    })
    system.actorOf(dbActorMock, "MockedDB-ACK")
  }


  def mockDbElectionNotSetUp: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.CreateChannel(_, _) | DbActor.CreateElectionData(_, _) =>
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
        case DbActor.ReadElectionData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadElectionDataAck(electionData)
      }
    })
    system.actorOf(dbActorMock, "MockedDB-ElectionNotCreated")
  }

  def mockDbElectionSecretBallotReadFailed: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.CreateChannel(_, _) | DbActor.CreateElectionData(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ChannelExists(_) =>
          system.log.info("Received a channel exist setup message")
          system.log.info("Responding with a no")
          sender() ! Status.Failure(DbActorNAckException(1, "no"))
        case DbActor.ReadLaoData(_) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
        case DbActor.ReadElectionData(_) =>
          system.log.info("Received a read election data")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "no"))
      }
    })
    system.actorOf(dbActorMock, "MockedDB-ElectionNotCreated")
  }

  test("SetupElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElection

    rc.handleSetupElection(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection should succeed if the election doesn't already exists in database") {
    val mockedDB = mockDbElectionNotSetUp
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElection

    rc.handleSetupElection(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection with secret ballot should succeed if the it is stored correctly in the database") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElectionSecretBallot

    rc.handleSetupElection(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("SetupElection with secret ballot should fail if the election cannot read in the election data") {
    val mockedDB = mockDbElectionSecretBallotReadFailed
    val rc = new ElectionHandler(mockedDB)
    val request = SetupElectionMessages.setupElectionSecretBallot

    rc.handleSetupElection(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should fail if the election does not exist") {
    val mockedDB = mockDbElectionNotSetUp
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("OpenElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = OpenElectionMessages.openElection

    rc.handleOpenElection(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("KeyElection should succeed if the election already exists") {
    val mockedDB = mockDbWithAck
    val rc = new ElectionHandler(mockedDB)
    val request = KeyElectionMessages.keyElection

    rc.handleKeyElection(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("keyElection should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new ElectionHandler(mockedDB)
    val request = KeyElectionMessages.keyElection

    rc.handleKeyElection(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }
}
