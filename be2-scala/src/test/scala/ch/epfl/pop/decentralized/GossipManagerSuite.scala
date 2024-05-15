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
import ch.epfl.pop.model.network.MethodType.rumor
import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
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

    peerServer.receiveOne(duration) match
      case ClientAnswer(Right(jsonRpcRequest: JsonRpcRequest)) =>
        jsonRpcRequest.method shouldBe MethodType.rumor
        jsonRpcRequest.id shouldBe Some(0)
        jsonRpcRequest.getParams.asInstanceOf[Rumor] shouldBe rumor
      case _ => 0 shouldBe 1
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
    for (peer <- peers) {
      connectionMediatorRef ? ConnectionMediator.NewServerConnected(peer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    }

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
        case null            => remainingPeers :+= peer
    }
    remainingPeers.size shouldBe peers.size - 1

    // sends back to the gossipManager a response that the rumor is new
    val response = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      ResultObject(0),
      Some(0)
    ))

    // by processing the reponse, gossipManager should send again a rumor to a new peer
    val outputResponse = Source.single(response).via(gossipMonitor).runWith(Sink.head)

    Await.result(outputResponse, duration)

    var remainingPeers2: List[TestProbe] = List.empty

    remainingPeers.foreach { peer =>
      peer.receiveOne(duration) match
        case ClientAnswer(_) =>
        case null            => remainingPeers2 :+= peer
    }
    remainingPeers2.size shouldBe remainingPeers.size - 1

  }

  test("When receiving a message, gossip manager should create and send a rumor") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
    val gossip = GossipManager.gossip(gossipManager)
    val peerServer = TestProbe()

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    // emulates receiving a castVote and processes it
    val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
    Await.result(outputCreateRumor, duration)

    // checks that created a correct rumor from that message and was received by other server
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

  test("Gossip manager increments jsonRpcId and rumorID when starting a gossip from message") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
    val gossip = GossipManager.gossip(gossipManager)
    val peerServer = TestProbe()

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    // emulates receiving a castVote and processes it
    for (id <- 0 to 4) {
      // emulates receiving a castVote and processes it
      val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
      Await.result(outputCreateRumor, duration)
      // checks that created a correct rumor from that message and was received by other server
      val rumor = Rumor(PublicKey(Base64Data("blabla")), id, Map(castVoteRequest.getParamsChannel -> List(castVoteRequest.getParamsMessage.get)))
      val receivedMsg = peerServer.receiveOne(duration).asInstanceOf[ClientAnswer]
      receivedMsg.graphMessage match
        case Right(jsonRpcRequest: JsonRpcRequest) =>
          jsonRpcRequest.id shouldBe Some(id)
          jsonRpcRequest.getParams.asInstanceOf[Rumor].rumorId shouldBe id
        case _ => 0 shouldBe 1
    }

  }

  test("Gossip manager should increment jsonRpcId but not rumor when starting gossip from rumor") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, monitorRef, connectionMediatorRef))
    val gossipHandler = GossipManager.gossipHandler(gossipManager)

    val peerServer = TestProbe()
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    for (id <- 0 to 4) {
      // emulates receiving a castVote and processes it
      val outputCreateRumor = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)
      Await.result(outputCreateRumor, duration)
      // checks that created a correct rumor from that message and was received by other server
      val receivedMsg = peerServer.receiveOne(duration).asInstanceOf[ClientAnswer]
      receivedMsg.graphMessage match
        case Right(jsonRpcRequest: JsonRpcRequest) =>
          jsonRpcRequest.id shouldBe Some(id)
          jsonRpcRequest.getParams.asInstanceOf[Rumor].rumorId shouldBe rumor.rumorId
        case _ => 0 shouldBe 1
    }
  }

}
