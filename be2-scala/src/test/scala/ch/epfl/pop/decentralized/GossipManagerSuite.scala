package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage, SecurityModuleActor}
import org.scalatest.funsuite.{AnyFunSuite, AnyFunSuiteLike}
import org.scalatest.matchers.should.Matchers
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.GraphMessage

import scala.concurrent.Await

class GossipManagerSuite extends TestKit(ActorSystem("GossipManagerSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with Matchers {

  val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  val messageRegistry: MessageRegistry = MessageRegistry()
  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "DbActor")
  val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
  val monitorRef: ActorRef = system.actorOf(Monitor.props(dbActorRef))
  val connectionMediatorRef: AskableActorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))

  val rumor: Rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  test("gossip handler should forward a rumor to a random server") {

    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
    val gossipHandler = GossipManager.gossipHandler(gossipManager)

    val peerServer = TestProbe()

    // register server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    val output = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(output, duration)

    peerServer.expectMsg(duration, ClientAnswer(Right(rumorRequest)))
  }

  test("gossip handler should send to only one server if multiples are present") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
    val gossipHandler = GossipManager.gossipHandler(gossipManager)

    val peerServer1 = TestProbe()
    val peerServer2 = TestProbe()
    val peerServer3 = TestProbe()
    val peerServer4 = TestProbe()

    val peers = List(peerServer1, peerServer2, peerServer3, peerServer4)

    // register server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer1.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer2.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer3.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer4.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    val output = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    peers.map(_.receiveOne(duration)).count(_ != null) shouldBe 1

  }

}
