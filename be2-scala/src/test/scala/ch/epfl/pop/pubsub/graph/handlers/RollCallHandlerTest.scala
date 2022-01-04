package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props}
import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import scala.concurrent.duration.FiniteDuration

import ch.epfl.pop.pubsub.graph.{DbActor, PipelineError}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.model.network.requests.rollCall.JsonRpcRequestCreateRollCall
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.objects.Channel

import util.examples.RollCallMessages

class RollCallHandlerTest extends TestKit(ActorSystem("RollCall-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicites for system actors
  implicit val duration = FiniteDuration(5 ,"seconds")
  implicit val timeout  = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  def mockDbWIthNack: AskableActorRef = {
    val mockedDB = Props(new Actor(){
          override def receive = {
              // You can modify the following match case to include more args, names...
              case m : DbActor.WriteAndPropagate =>
                system.log.info("Received {}", m)
                system.log.info("Responding with a Nack")

                sender ! DbActor.DbActorNAck(1, "error")
          }
        }
      )
    system.actorOf(mockedDB, "MockedDB-NACK")
  }

  def mockDbWIthAck: AskableActorRef = {
    val mockedDB = Props(new Actor(){
          override def receive = {
              // You can modify the following match case to include more args, names...
              case m : DbActor.WriteAndPropagate =>
                system.log.info("Received {}", m)
                system.log.info("Responding with a Ack")

                sender ! DbActor.DbActorWriteAck()
          }
        }
      )
    system.actorOf(mockedDB, "MockedDB-ACK")
  }

  test("CreateRollCall fails if the database fails storing the message"){
    val mockedDB = mockDbWIthNack
    val rc = new RollCallHandler(mockedDB)
    val request = RollCallMessages.createRollCall

    rc.handleCreateRollCall(request) shouldBe an [Right[PipelineError,_]]

    system.stop(mockedDB.actorRef)
  }

  test("CreateRollCall succeeds if the database succeeds storing the message"){
    val mockedDB = mockDbWIthAck
    val rc = new RollCallHandler(mockedDB)
    val request = RollCallMessages.createRollCall

    rc.handleCreateRollCall(request) should equal (Left(request))

    system.stop(mockedDB.actorRef)
  }
  //TODO Add more RollCall handlers test

}
