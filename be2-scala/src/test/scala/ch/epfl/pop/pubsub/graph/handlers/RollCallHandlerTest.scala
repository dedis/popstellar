package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.data.{CloseRollCallMessages, CreateRollCallMessages, OpenRollCallMessages, ReopenRollCallMessages}

import scala.concurrent.duration.FiniteDuration

class RollCallHandlerTest extends TestKit(ActorSystem("RollCall-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  def mockDbWithNack: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m @ (DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) | DbActor.AssertChannelMissing(_) | DbActor.WriteRollCallData(_, _) | DbActor.WriteLaoData(_, _, _)) =>
          system.log.info(s"Received - message $m")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbRollCallNotCreated: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.CreateChannel(_, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.WriteRollCallData(_, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ChannelExists(_) =>
          system.log.info(s"Received a create rollcall message")
          system.log.info("Responding with a no")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case DbActor.AssertChannelMissing(_) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbRollCallAlreadyCreated: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.WriteRollCallData(_, _) | DbActor.WriteLaoData(_, _, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.AssertChannelMissing(_) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a no")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  test("CreateRollCall should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall
    rc.handleCreateRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall should succeed if the rollcall doesn't already exist in the database") {
    val mockedDB = mockDbRollCallNotCreated
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall
    rc.handleCreateRollCall(request) should matchPattern { case Left(_) => }
    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall should fail if the rollcall already exists in database") {
    val mockedDB = mockDbRollCallAlreadyCreated
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall
    rc.handleCreateRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

  test("OpenRollCall should fail if the rollcall does not exist in database") {
    val mockedDB = mockDbRollCallNotCreated
    val rc = new RollCallHandler(mockedDB)
    val request = OpenRollCallMessages.openRollCall
    rc.handleOpenRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

  test("OpenRollCall should succeed if the rollcall is already created") {
    val mockedDB = mockDbRollCallAlreadyCreated
    val rc = new RollCallHandler(mockedDB)
    val request = OpenRollCallMessages.openRollCall
    rc.handleOpenRollCall(request) should matchPattern { case Left(_) => }
    system.stop(mockedDB.actorRef)
  }

  test("OpenRollCall should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = OpenRollCallMessages.openRollCall
    rc.handleOpenRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

  test("ReopenRollcall should succeed if the rollcall is already created") {
    val mockedDB = mockDbRollCallAlreadyCreated
    val rc = new RollCallHandler(mockedDB)
    val request = ReopenRollCallMessages.reopenRollCall
    rc.handleReopenRollCall(request) should matchPattern { case Left(_) => }
    system.stop(mockedDB.actorRef)
  }

  test("ReopenRollcall should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = ReopenRollCallMessages.reopenRollCall
    rc.handleReopenRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

  test("CloseRollcall should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = CloseRollCallMessages.closeRollCall
    rc.handleCloseRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

}
