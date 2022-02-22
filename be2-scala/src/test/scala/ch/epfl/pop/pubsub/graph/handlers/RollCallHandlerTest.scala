package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActorNew
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
        case m: DbActorNew.WriteAndPropagate =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))
      }
    })
    system.actorOf(dbActorMock, "MockedDB-NACK")
  }

  def mockDbWithAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m: DbActorNew.WriteAndPropagate =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActorNew.DbActorAck()
      }
    })
    system.actorOf(dbActorMock, "MockedDB-ACK")
  }

  test("CreateRollCall fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall

    rc.handleCreateRollCall(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall succeeds if the database succeeds storing the message") {
    val mockedDB = mockDbWithAck
    val rc = new RollCallHandler(mockedDB)
    val request = CreateRollCallMessages.createRollCall

    rc.handleCreateRollCall(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }
  //TODO Add more RollCall handlers test

}
