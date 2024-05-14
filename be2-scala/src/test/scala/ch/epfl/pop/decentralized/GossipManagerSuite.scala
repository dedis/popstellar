package ch.epfl.pop.decentralized

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.decentralized.{ConnectionMediator, GossipManager, Monitor}
import ch.epfl.pop.model.network.MethodType.rumor
import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
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

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var securityModuleActorRef: AskableActorRef = _
  private var monitorRef: ActorRef = _
  private var connectionMediatorRef: ActorRef = _
  private var gossipManager: AskableActorRef = _

  override def beforeEach(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props)
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
    securityModuleActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
    monitorRef = system.actorOf(Monitor.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

    gossipManager = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
  }

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor.json"
  val pathCorrectCastVote: String = "src/test/scala/util/examples/json/election/cast_vote1.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))
  val castVoteRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectCastVote))

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


  test("gossip handler should send rumor if there is an ongoing gossip protocol") {
    val gossipHandler = GossipManager.gossipHandler(gossipManager)
    val gossipMonitor = GossipManager.monitorResponse(gossipManager)

    val peerServer1 = TestProbe()
    val peerServer2 = TestProbe()
    val peerServer3 = TestProbe()
    val peerServer4 = TestProbe()

    val peers = List(peerServer1, peerServer2, peerServer3, peerServer4)

    // register server
    for (peer <- peers) {
      connectionMediatorRef ? ConnectionMediator.NewServerConnected(peer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    }

    // processes the rumor => sends to random peer
    val outputRumor = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(outputRumor, duration.mul(2))

    var remainingPeers: List[TestProbe] = List.empty

    // checks that only one peers received the rumor
    peers.foreach { peer =>
      peer.receiveOne(duration.mul(2)) match
        case ClientAnswer(_) =>
        case null => remainingPeers :+= peer
    }
    remainingPeers.size shouldBe peers.size - 1

    // sends back to the gossipManager a response that the rumor is new
    val response = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      ResultObject(0),
      rumorRequest.id
    ))

    // by processing the reponse, gossipManager should send again a rumor to a new peer
    val outputResponse = Source.single(response).via(gossipMonitor).runWith(Sink.head)

    Await.result(outputResponse, duration.mul(2))

    var remainingPeers2: List[TestProbe] = List.empty

    remainingPeers.foreach { peer =>
      peer.receiveOne(duration.mul(2)) match
        case ClientAnswer(_) =>
        case null => remainingPeers2 :+= peer
    }
    remainingPeers2.size shouldBe remainingPeers.size-1

  }

  test("When receiving a message, gossip manager should create and send a rumor") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))

    val gossip = GossipManager.gossip(gossipManager)

    val peerServer = TestProbe()

    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)

    Await.result(outputCreateRumor, duration)

    val rumor = Rumor(PublicKey(Base64Data("blabla")), 0, Map(castVoteRequest.getParamsChannel -> List(castVoteRequest.getParamsMessage.get)))

    val receivedMsg = peerServer.receiveOne(duration).asInstanceOf[ClientAnswer]

    receivedMsg.graphMessage match
      case Right(jsonRpcRequest: JsonRpcRequest) =>
        jsonRpcRequest.method shouldBe MethodType.rumor
        jsonRpcRequest.id shouldBe Some(0)
        val jsonRumor = jsonRpcRequest.getParams.asInstanceOf[Rumor]
        jsonRumor shouldBe rumor
      case _ => 0 shouldBe 1

  }

}
