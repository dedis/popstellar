package ch.epfl.pop.json

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.decentralized.{ConnectionMediator, GossipManager, Monitor}
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultObject}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage, SecurityModuleActor}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Await

class GossipManagerSuite extends TestKit(ActorSystem("GossipManagerSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with Matchers with BeforeAndAfterEach with BeforeAndAfterAll{

  private val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  private val messageRegistry: MessageRegistry = MessageRegistry()
  private val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props)
  private val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
  private val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
  private val monitorRef: ActorRef = system.actorOf(Monitor.props(dbActorRef))
  private val connectionMediatorRef: ActorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))
  private val gossipManager: AskableActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))


  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

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

    Await.result(output, duration)

    peers.map(_.receiveOne(duration)).count(_ != null) shouldBe 1

  }

  test("gossip handler should send rumor if there is an ongoing gossip protocol") {
    val gossipHandler = GossipManager.gossipHandler(gossipManager)
    val gossipMonitor = GossipManager.monitorResponse(gossipManager)

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
    val outputRumor = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(outputRumor, duration)

    val received = peers.map(_.receiveOne(duration))

    received.count(_ != null) shouldBe 1

    val remainingPeers = peers.lazyZip(received).filter((_, recv) => recv == null).map(_._1)

    remainingPeers.size shouldBe peers.size - 1

    val response = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      ResultObject(0),
      rumorRequest.id
    ))

    val outputResponse = Source.single(response).via(gossipMonitor).runWith(Sink.head)

    Await.result(outputResponse, duration)
    remainingPeers.map(_.receiveOne(duration)).count(_ != null) shouldBe 1

  }




}
