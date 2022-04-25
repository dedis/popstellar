package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.FSM.Failure
import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.ChannelExists
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.data.CreateRollCallMessages

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
        case DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) => sender() ! DbActor.DbActorAck()
        case DbActor.WriteAndPropagate(_, _) =>
          system.log.info(f"Received a write and propagate")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-NACK")
  }

  def mockDbRollCallNotCreated: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.CreateChannel(_, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.ChannelExists(_) =>
          system.log.info(s"Received a create rollcall message")
          system.log.info("Responding with a no")
          // fixme : replace the 2 next lines with
          //  sender() ! Status.Failure(DbActorNAckException(1, "error"))
          //  but it is crashing
          val oddNack = DbActor.DbActorCatchupAck
          sender() ! oddNack
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-RollCallNotCreated")
  }

  def mockDbRollCallAlreadyCreated: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(_, _) | DbActor.ChannelExists(_) | DbActor.CreateChannel(_, _) =>
          system.log.info(s"Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-RollCallAlreadyCreated")
  }

  test("CreateRollCall should fail if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall

    rc.handleCreateRollCall(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall should succeed if the roll call doesn't already exists in database") {
    val mockedDB = mockDbRollCallNotCreated
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall
    rc.handleCreateRollCall(request) should matchPattern { case Left(_) => }
    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall should fail if the roll call already exists in database") {
    val mockedDB = mockDbRollCallAlreadyCreated
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall
      rc.handleCreateRollCall(request) shouldBe an[Right[PipelineError, _]]
    system.stop(mockedDB.actorRef)
  }

}
