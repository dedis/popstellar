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
import ch.epfl.pop.model.network.method.{GreetServer, Rumor, RumorState}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey, RumorData}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.{GraphMessage, MessageDecoder}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorReadRumorData}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class GossipManagerSuite extends TestKit(ActorSystem("GossipManagerSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with Matchers with BeforeAndAfterEach {

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var securityModuleActorRef: AskableActorRef = _
  private var monitorRef: ActorRef = _
  private var connectionMediatorRef: AskableActorRef = _
  private val fakeRef = TestProbe().ref

  override def beforeEach(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props)
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
    securityModuleActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
    monitorRef = system.actorOf(Monitor.props(dbActorRef))

  }

  def checkPeersWritten(connectionMediatorRef: AskableActorRef): Unit = {
    var countDown = 2
    while {
      val readPeers = connectionMediatorRef ? ConnectionMediator.ReadPeersClientAddress()
      Await.result(readPeers, duration) match
        case ConnectionMediator.ReadPeersClientAddressAck(list) => list.isEmpty && countDown > 0
    } do countDown -= 1
  }

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor.json"
  val pathCorrectCastVote: String = "src/test/scala/util/examples/json/election/cast_vote1.json"
  val pathCorrectFederationInit: String = "src/test/scala/util/examples/json/federation/federation_init.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))
  val castVoteRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectCastVote))
  val federationInitRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectFederationInit))

  val rumor: Rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  test("When receiving a message, gossip manager should create and send a rumor") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe("a")
    val gossip = GossipManager.startGossip(gossipManager, sender.ref)

    val server = TestProbe()

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(server.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    checkPeersWritten(connectionMediatorRef)

    // emulates receiving a castVote and processes it
    val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
    Await.result(outputCreateRumor, duration)
    val publicKey: Option[PublicKey] = {
      val readPk = dbActorRef ? DbActor.ReadServerPublicKey()
      Await.result(readPk, duration) match
        case DbActor.DbActorReadServerPublicKeyAck(pk) => Some(pk)
        case _                                         => None
    }

    // checks that created a correct rumor from that message and was received by other server
    val rumor = Rumor(publicKey.get, 0, Map(castVoteRequest.getParamsChannel -> List(castVoteRequest.getParamsMessage.get)))
    // ignore state
    server.receiveOne(duration)

    val receivedMsg = server.receiveOne(duration.mul(5)).asInstanceOf[ClientAnswer]
    receivedMsg.graphMessage match
      case Right(jsonRpcRequest: JsonRpcRequest) =>
        jsonRpcRequest.method shouldBe MethodType.rumor
        jsonRpcRequest.id shouldBe Some(1)
        val jsonRumor = jsonRpcRequest.getParams.asInstanceOf[Rumor]
        jsonRumor shouldBe rumor
      case _ => 0 shouldBe 1
  }

  test("When receiving ignored message, doesn't start gossiping") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe("a")
    val gossip = GossipManager.startGossip(gossipManager, sender.ref)

    val server = TestProbe()

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(server.ref, GreetServer(PublicKey(Base64Data("")), "", ""))

    checkPeersWritten(connectionMediatorRef)

    val decodedInit = MessageDecoder.parseData(Right(federationInitRequest), messageRegistry)

    // emulates receiving a federationInit and processes it
    val outputCreateRumor = Source.single(decodedInit).via(gossip).runWith(Sink.head)
    Await.result(outputCreateRumor, duration)

    // ignore state
    server.receiveOne(duration)

    server.expectNoMessage()
  }

  test("Gossip manager increments jsonRpcId and rumorID when starting a gossip from message") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe("b")
    val gossip = GossipManager.startGossip(gossipManager, sender.ref)

    val peerServer = TestProbe()
    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    checkPeersWritten(connectionMediatorRef)
    // ignore state
    peerServer.receiveOne(duration)

    // emulates receiving a castVote and processes it
    for (id <- 0 to 4) {
      // emulates receiving a castVote and processes it
      println("castVote")
      val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
      Await.result(outputCreateRumor, duration)
      // checks that created a correct rumor from that message and was received by other server
      val rumor = Rumor(PublicKey(Base64Data("blabla")), id, Map(castVoteRequest.getParamsChannel -> List(castVoteRequest.getParamsMessage.get)))

      val receivedMsg = peerServer.receiveOne(duration).asInstanceOf[ClientAnswer]
      receivedMsg.graphMessage match
        case Right(jsonRpcRequest: JsonRpcRequest) =>
          jsonRpcRequest.id shouldBe Some(id + 1)
          jsonRpcRequest.getParams.asInstanceOf[Rumor].rumorId shouldBe id
        case _ => 0 shouldBe 1
    }

  }

  test("Gossip manager should increment jsonRpcId but not rumor when starting gossip from rumor") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe("c")
    val gossipHandler = GossipManager.gossipHandler(gossipManager, sender.ref)

    val peerServer = TestProbe()
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    checkPeersWritten(connectionMediatorRef)
    // ignore state
    peerServer.receiveOne(duration)

    for (id <- 0 to 4) {
      // emulates receiving a castVote and processes it
      val rumorRpc = JsonRpcRequest(
        RpcValidator.JSON_RPC_VERSION,
        MethodType.rumor,
        Rumor(rumor.senderPk, id, rumor.messages),
        Some(0)
      )
      val outputCreateRumor = Source.single(Right(rumorRpc)).via(gossipHandler).runWith(Sink.head)
      Await.result(outputCreateRumor, duration)
      // checks that created a correct rumor from that message and was received by other server
      val receivedMsg = peerServer.receiveOne(duration).asInstanceOf[ClientAnswer]
      receivedMsg.graphMessage match
        case Right(jsonRpcRequest: JsonRpcRequest) =>
          jsonRpcRequest.id shouldBe Some(id + 1)
          jsonRpcRequest.getParams.asInstanceOf[Rumor].rumorId shouldBe id
        case _ => 0 shouldBe 1
    }
  }

  test("Gossip should stop when there is no peers left") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    val sender = TestProbe("d")
    val gossip = GossipManager.startGossip(gossipManager, sender.ref)
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))

    val peerServer = TestProbe()
    val gossipMonitor = GossipManager.monitorResponse(gossipManager)

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data.encode("publicKey")), "", ""))
    checkPeersWritten(connectionMediatorRef)
    // ignore state
    peerServer.receiveOne(duration)

    // emulates receiving a castVote and processes it
    val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
    Await.result(outputCreateRumor, duration)

    peerServer.receiveOne(duration) match
      case ClientAnswer(_) =>
      case _               => 0 shouldBe 1

    // sends back to the gossipManager a response that the rumor is new
    val response = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      ResultObject(0),
      Some(1)
    ))

    // by processing the reponse, gossipManager should not send again a rumor to a peer
    val outputResponse = Source.single(response).via(gossipMonitor).runWith(Sink.head)

    Await.result(outputResponse, duration)

    peerServer.expectNoMessage(duration)

  }

  test("Gossip should write in memory new rumors sent") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    val sender = TestProbe()
    val gossip = GossipManager.startGossip(gossipManager, sender.ref)
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))

    val peerServer = TestProbe()

    // registers a new server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data.encode("publicKey")), "", ""))
    checkPeersWritten(connectionMediatorRef)
    // ignore state
    peerServer.receiveOne(duration)

    // emulates receiving a castVote and processes it
    val outputCreateRumor = Source.single(Right(castVoteRequest)).via(gossip).runWith(Sink.head)
    Await.result(outputCreateRumor, duration)

    val readGossipPk = dbActorRef ? DbActor.ReadServerPublicKey()
    val gossipPk =
      Await.result(readGossipPk, duration) match
        case DbActor.DbActorReadServerPublicKeyAck(pk) => pk
        case _                                         => 0 shouldBe 1

    peerServer.receiveOne(duration)

    val readRumorId = dbActorRef ? DbActor.ReadRumorData(gossipPk.asInstanceOf[PublicKey])
    val rumorId =
      Await.result(readRumorId, duration) match
        case DbActorReadRumorData(foundRumorIds: RumorData) =>
          foundRumorIds.lastRumorId() shouldBe 0
        case _ => 0 shouldBe 1
  }

  test("gossip handler should forward a rumor to a random server") {

    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe()
    val gossipHandler = GossipManager.gossipHandler(gossipManager, sender.ref)

    val peerServer = TestProbe()

    // register server
    connectionMediatorRef ? ConnectionMediator.NewServerConnected(peerServer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
    checkPeersWritten(connectionMediatorRef)
    // ignore state
    peerServer.receiveOne(duration)

    val output = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(output, duration)

    peerServer.receiveOne(duration) match
      case ClientAnswer(Right(jsonRpcRequest: JsonRpcRequest)) =>
        jsonRpcRequest.method shouldBe MethodType.rumor
        jsonRpcRequest.id shouldBe Some(1)
        jsonRpcRequest.getParams.asInstanceOf[Rumor] shouldBe rumor
      case _ => 0 shouldBe 1
  }

  test("gossip handler should send to only one server if multiples are present") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe()
    val gossipHandler = GossipManager.gossipHandler(gossipManager, sender.ref)

    val peerServer1 = TestProbe()
    val peerServer2 = TestProbe()
    val peerServer3 = TestProbe()
    val peerServer4 = TestProbe()

    val peers = List(peerServer1, peerServer2, peerServer3, peerServer4)

    // register server
    for (peer <- peers) {
      connectionMediatorRef ? ConnectionMediator.NewServerConnected(peer.ref, GreetServer(PublicKey(Base64Data("")), "", ""))
      peer.receiveOne(duration)
    }
    checkPeersWritten(connectionMediatorRef)

    val output = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(output, duration)

    peers.map(_.receiveOne(duration)).count(_ != null) shouldBe 1

  }

  test("gossip handler should send rumor if there is an ongoing gossip protocol") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, pullRate = 30.seconds))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val sender = TestProbe()
    val gossipHandler = GossipManager.gossipHandler(gossipManager, sender.ref)

    val gossipMonitor = GossipManager.monitorResponse(gossipManager)

    val peerServer1 = TestProbe()
    val peerServer2 = TestProbe()
    val peerServer3 = TestProbe()
    val peerServer4 = TestProbe()

    val peers = List(peerServer1, peerServer2, peerServer3, peerServer4)

    // register server
    var n = 0
    for (peer <- peers) {
      connectionMediatorRef ? ConnectionMediator.NewServerConnected(peer.ref, GreetServer(PublicKey(Base64Data.encode(s"$n")), "", ""))
      peer.receiveOne(duration)
      n += 1
    }
    checkPeersWritten(connectionMediatorRef)

    // processes the rumor => sends to random peer
    val outputRumor = Source.single(Right(rumorRequest)).via(gossipHandler).runWith(Sink.head)

    Await.result(outputRumor, duration.mul(2))

    var remainingPeers: List[TestProbe] = List.empty

    // checks that only one peers received the rumor
    peers.foreach { peer =>
      peer.receiveOne(duration.mul(2)) match
        case ClientAnswer(Right(jsonRpcRequest: JsonRpcRequest)) if jsonRpcRequest.method == MethodType.rumor =>
        case _                                                                                                => remainingPeers :+= peer
    }
    remainingPeers.size shouldBe peers.size - 1

    // sends back to the gossipManager a response that the rumor is new
    val response = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      ResultObject(0),
      Some(4)
    ))

    // by processing the reponse, gossipManager should send again a rumor to a new peer
    val outputResponse = Source.single(response).via(gossipMonitor).runWith(Sink.head)

    Await.result(outputResponse, duration)

    var remainingPeers2: List[TestProbe] = List.empty

    remainingPeers.foreach { peer =>
      peer.receiveOne(duration) match
        case ClientAnswer(Right(jsonRpcRequest: JsonRpcRequest)) if jsonRpcRequest.method == MethodType.rumor =>
        case _                                                                                                => remainingPeers2 :+= peer
    }
    remainingPeers2.size shouldBe remainingPeers.size - 1

  }

  test("Gossip sends rumor state when there is one server connected") {
    val gossipManager: ActorRef = system.actorOf(GossipManager.props(dbActorRef, pullRate = 2.seconds))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, gossipManager, messageRegistry))
    val server = TestProbe()

    val writeRumor = dbActorRef ? DbActor.WriteRumor(rumor)
    Await.result(writeRumor, duration) shouldBe DbActorAck()

    connectionMediatorRef ? ConnectionMediator.NewServerConnected(server.ref, GreetServer(PublicKey(Base64Data.encode("publickey")), "client", "server"))
    checkPeersWritten(connectionMediatorRef)

    server.receiveOne(5.seconds) match
      case ClientAnswer(Right(jsonRpcRequest: JsonRpcRequest)) =>
        jsonRpcRequest.id shouldBe Some(0)
        jsonRpcRequest.method shouldBe MethodType.rumor_state
        val rumorState = jsonRpcRequest.getParams.asInstanceOf[RumorState]
        rumorState.state shouldBe Map(rumor.senderPk -> rumor.rumorId)
  }

}
