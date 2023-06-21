package ch.epfl.pop.pubsub.graph

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.model.network.method.GreetServer
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}

import scala.concurrent.duration.DurationInt

class ClientActorSuite extends TestKit(ActorSystem("ClientActorSuiteSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll with AskPatternConstants {

  private val timeout = 3.second

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("When created with initGreet set to true, the clientActor should send a NewServerConnected to the connectionMediator when receiving back a greetServer") {
    val clientAddress = RuntimeEnvironment.ownClientAddress
    val serverAddress = RuntimeEnvironment.ownServerAddress
    val greetServer: GreetServer = GreetServer(PublicKey(Base64Data("0000")), clientAddress, serverAddress)
    val mediator = TestProbe()
    val connectionMediator = TestProbe()
    val clientActor: ActorRef = system.actorOf(ClientActor.props(mediator.ref, connectionMediator.ref, true, true))
    val expectedNewServerConnected = ConnectionMediator.NewServerConnected(clientActor, greetServer)
    clientActor ! greetServer
    connectionMediator.expectMsg(timeout, expectedNewServerConnected)
  }

  test("When created with initGreet set to false, the clientActor should inform the connection Mediator of the new opened connection when receiving the greetServer") {
    val expectedClientAddress = RuntimeEnvironment.ownClientAddress
    val expectedServerAddress = RuntimeEnvironment.ownServerAddress
    val expectedGreetServer = GreetServer(PublicKey(Base64Data("0000")), expectedClientAddress, expectedServerAddress)
    val mediator = TestProbe()
    val connectionMediator = TestProbe()
    val clientActor: ActorRef = system.actorOf(ClientActor.props(mediator.ref, connectionMediator.ref, true, false))
    val expectedNewServerConnected = ConnectionMediator.NewServerConnected(clientActor, expectedGreetServer)
    clientActor ! expectedGreetServer
    connectionMediator.expectMsg(timeout, expectedNewServerConnected)
  }

  test("when initGreet set to true, ClientActor should wait for the greetServer to come before sending a NewServerConnected message") {
    val mediator = TestProbe()
    val connectionMediator = TestProbe()
    val clientActor: ActorRef = system.actorOf(ClientActor.props(mediator.ref, connectionMediator.ref, true, false))
    connectionMediator.expectNoMessage()
  }

}
