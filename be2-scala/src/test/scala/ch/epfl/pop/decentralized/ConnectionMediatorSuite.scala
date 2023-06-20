package ch.epfl.pop.decentralized

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.method.{GreetServer, Heartbeat}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientActorMessage
import ch.epfl.pop.pubsub.MessageRegistry
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class ConnectionMediatorSuite extends TestKit(ActorSystem("ConnectionMediatorSuiteActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll {
  private val timeout = 3.seconds

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("ConnectionMediator tells monitor if some or no servers are connected") {
    val mockMonitor = TestProbe()
    val server = TestProbe()

    val connectionMediatorRef = system.actorOf(
      ConnectionMediator.props(mockMonitor.ref, ActorRef.noSender, ActorRef.noSender, MessageRegistry())
    )

    mockMonitor.expectMsg(timeout, ConnectionMediator.Ping())

    // Register server
    connectionMediatorRef ! ConnectionMediator.NewServerConnected(server.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    mockMonitor.expectMsg(timeout, Monitor.AtLeastOneServerConnected)

    // Unregister server
    connectionMediatorRef ! ConnectionMediator.ServerLeft(server.ref)

    mockMonitor.expectMsg(timeout, Monitor.NoServerConnected)
  }

  test("ConnectionMediator broadcasts heartbeat only to the expected servers") {

    val mockMonitor = TestProbe()
    val server1 = TestProbe()
    val server2 = TestProbe()
    val server3 = TestProbe()

    val connectionMediatorRef = system.actorOf(
      ConnectionMediator.props(mockMonitor.ref, ActorRef.noSender, ActorRef.noSender, MessageRegistry())
    )

    mockMonitor.expectMsg(timeout, ConnectionMediator.Ping())

    // Register servers
    connectionMediatorRef ! ConnectionMediator.NewServerConnected(server1.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    connectionMediatorRef ! ConnectionMediator.NewServerConnected(server2.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    connectionMediatorRef ! ConnectionMediator.NewServerConnected(server3.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    // Monitor expect a single message
    mockMonitor.expectMsg(timeout, Monitor.AtLeastOneServerConnected)
    mockMonitor.expectNoMessage(timeout)

    // Ask to broadcast Heartbeat
    connectionMediatorRef ! new Heartbeat(Map.empty)

    server1.expectMsgType[ClientActorMessage]
    server2.expectMsgType[ClientActorMessage]
    server3.expectMsgType[ClientActorMessage]

    // Some server leaves
    connectionMediatorRef ! ConnectionMediator.ServerLeft(server1.ref)
    connectionMediatorRef ! ConnectionMediator.ServerLeft(server2.ref)

    // Ask to broadcast Heartbeat
    connectionMediatorRef ! new Heartbeat(Map.empty)

    server1.expectNoMessage(timeout)
    server2.expectNoMessage(timeout)
    server3.expectMsgType[ClientActorMessage](timeout)

    // Everybody leaves
    connectionMediatorRef ! ConnectionMediator.ServerLeft(server3.ref)
    mockMonitor.expectMsg(timeout, Monitor.NoServerConnected)
  }

  test("Connection Mediator return an empty client address list when no peers are connected") {
    val mockMonitor = TestProbe()
    val testProbe = TestProbe()
    val connectionMediatorRef = system.actorOf(
      ConnectionMediator.props(mockMonitor.ref, ActorRef.noSender, ActorRef.noSender, MessageRegistry())
    )

    testProbe.send(connectionMediatorRef, ConnectionMediator.ReadPeersClientAddress())

    testProbe.expectMsg(timeout, ConnectionMediator.ReadPeersClientAddressAck(List.empty))
  }
}
