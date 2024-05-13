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
import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse, ResultObject}
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await

class GossipManagerSuite extends TestKit(ActorSystem("GossipManagerSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with Matchers with BeforeAndAfterEach {

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var securityModuleActorRef: AskableActorRef = _
  private var monitorRef: ActorRef = _
  private var connectionMediatorRef: AskableActorRef = _

  override def beforeEach(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props)
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
    securityModuleActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
    monitorRef = system.actorOf(Monitor.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

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
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
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
