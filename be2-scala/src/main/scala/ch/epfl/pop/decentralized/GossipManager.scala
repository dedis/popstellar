package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.slf4j.Logger
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.decentralized.{ConnectionMediator, GossipManager}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GreetServer, Rumor, RumorState}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType}
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey, RumorData}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorReadRumor, DbActorReadRumorData}
import akka.pattern.ask

import scala.concurrent.Await
import scala.util.{Failure, Random, Success}

final case class GossipManager(
    dbActorRef: AskableActorRef,
    monitorRef: ActorRef,
    stopProbability: Double = 0.5
) extends Actor with AskPatternConstants with ActorLogging {

  private type ServerInfos = (ActorRef, GreetServer)
  private var activeGossipProtocol: Map[JsonRpcRequest, Set[ServerInfos]] = Map.empty
  private var rumorMap: Map[PublicKey, Int] = Map.empty
  private var jsonId = 0
  private var publicKey: Option[PublicKey] = None
  private var connectionMediatorRef: AskableActorRef = _

  publicKey = {
    val readPk = dbActorRef ? DbActor.ReadServerPublicKey()
    Await.result(readPk, duration) match
      case DbActor.DbActorReadServerPublicKeyAck(pk) => Some(pk)
      case _ =>
        log.info(s"Actor (gossip) $self will not be able to create rumors because it has no publicKey")
        None
  }

  rumorMap =
    publicKey match
      case Some(pk: PublicKey) =>
        val readRumorData = dbActorRef ? DbActor.ReadRumorData(pk)
        Await.result(readRumorData, duration) match
          case DbActorReadRumorData(foundRumorIds: RumorData) => rumorMap.updated(pk, foundRumorIds.lastRumorId())
          case failure                                        => Map.empty
      case None => Map.empty

  monitorRef ! GossipManager.Ping()

  private def getPeersForRumor(jsonRpcRequest: JsonRpcRequest): Set[ServerInfos] = {
    val activeGossip = activeGossipProtocol.get(jsonRpcRequest)
    activeGossip match
      case Some(peersInfosList) => peersInfosList
      case None                 => Set.empty
  }

  private def prepareRumor(rumor: Rumor): JsonRpcRequest = {
    val request = JsonRpcRequest(
      RpcValidator.JSON_RPC_VERSION,
      MethodType.rumor,
      rumor,
      Some(jsonId)
    )
    jsonId += 1
    request
  }

  private def isNextRumor(publicKey: PublicKey, rumorId: Int): Boolean = {
    rumorMap.get(publicKey) match
      case None               => rumorId == 0
      case Some(localRumorId) => localRumorId == rumorId - 1
  }

  private def incrementMap(publicKey: PublicKey): Unit = {
    rumorMap = rumorMap.updated(publicKey, rumorMap.getOrElse(publicKey, -1) + 1)
  }

  /** Does a step of gossipping protocol for given rpc. Tries to find a random peer that hasn't already received this msg If such a peer is found, sends message and updates table accordingly. If no peer is found, ends the protocol.
    * @param rumorRpc
    *   Rpc that must be spreac
    */
  private def updateGossip(rumorRpc: JsonRpcRequest, clientActorRef: ActorRef): Unit = {
    // checks the peers to which we already forwarded the message
    val activeGossip: Set[ServerInfos] = getPeersForRumor(rumorRpc)
    // selects a random peer from remaining peers
    println(s"connmedref ${connectionMediatorRef.actorRef}")
    val randomPeer = connectionMediatorRef ? ConnectionMediator.GetRandomPeer(activeGossip.map(_._1) + clientActorRef)
    Await.result(randomPeer, duration) match {
      // updates the list based on response
      // if some peers are available we send
      case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
        println("foundpeer")
        val alreadySent: Set[ServerInfos] = activeGossip + (serverRef -> greetServer)
        activeGossipProtocol += (rumorRpc -> alreadySent)
        log.info(s"rumorSent > dest : ${greetServer.clientAddress}, rumor : $rumorRpc")
        serverRef ! ClientAnswer(
          Right(rumorRpc)
        )
      // else remove entry
      case ConnectionMediator.NoPeer() =>
        println("nopeer")
        activeGossipProtocol = activeGossipProtocol.removed(rumorRpc)
      case _ =>
        log.info(s"Actor $self received an unexpected message waiting for a random peer")
    }
  }

  /** When receiving a rumor that must be relayed, empacks a rumor in a new jsonRPC and tries to do a step of gossipping protocol
    * @param request
    */
  private def handleRumor(request: JsonRpcRequest, clientActorRef: ActorRef): Unit = {
    val rcvRumor = request.getParams.asInstanceOf[Rumor]
    if (isNextRumor(rcvRumor.senderPk, rcvRumor.rumorId))
      val newRumorRequest = prepareRumor(rcvRumor)
      incrementMap(rcvRumor.senderPk)
      updateGossip(newRumorRequest, clientActorRef)
    else
      val expectedId =
        rumorMap.get(rcvRumor.senderPk) match
          case None     => 0
          case Some(id) => id + 1
      log.info(s"Gossip Manager received an unexpected rumor $rcvRumor that doesn't match expected rumor id : $expectedId")
  }

  /** Processes a response. If a response matches a active gossip protocol, uses the reponse to decide how to continue gossipping If response is Positive (Result(0)), tries to do another step of gossipping If response is Negative (Error(-3)), considers stop gossiping
    * @param response
    *   Received response
    */
  private def processResponse(response: JsonRpcResponse, clientActorRef: ActorRef): Unit = {
    val activeGossipPeers = activeGossipProtocol.filter((k, _) => k.id == response.id)

    // response is expected because only one entry exists
    if (activeGossipPeers.size == 1) {
      activeGossipPeers.foreach { (rumorRpc, _) =>
        if (response.result.isEmpty && Random.nextDouble() < stopProbability) {
          activeGossipProtocol -= rumorRpc
        } else {
          updateGossip(rumorRpc, clientActorRef)
        }
      }
    } else {
      log.info(s"Unexpected match for active gossip. Response with id ${response.id} matched with ${activeGossipPeers.size} entries")
      // removes duplicate entries to come back to a stable state
      activeGossipPeers.foreach { (rumorRpc, _) =>
        activeGossipProtocol -= rumorRpc
      }
    }
  }

  private def startGossip(messages: Map[Channel, List[Message]], clientActorRef: ActorRef): Unit = {
    if (publicKey.isDefined)
      incrementMap(publicKey.get)
      println(s"rumorMap : $rumorMap")
      val rumor: Rumor = Rumor(publicKey.get, rumorMap(publicKey.get), messages)
      val jsonRpcRequest = prepareRumor(rumor)
      val writeRumor = dbActorRef ? DbActor.WriteRumor(rumor)
      Await.result(writeRumor, duration) match
        case DbActorAck() => updateGossip(jsonRpcRequest, clientActorRef)
        case _            => log.info(s"Actor (gossip) $self was not able to write rumor in memory. Gossip has not started.")
    else
      log.info(s"Actor (gossip) $self will not be able to start rumors because it has no publicKey")
  }

  override def receive: Receive = {
    case GossipManager.HandleRumor(jsonRpcRequest: JsonRpcRequest, clientActorRef: ActorRef) =>
      log.info("recvHandle")
      handleRumor(jsonRpcRequest, clientActorRef)

    case GossipManager.ManageGossipResponse(jsonRpcResponse, clientActorRef) =>
      log.info("recvManage")
      processResponse(jsonRpcResponse, clientActorRef)

    case GossipManager.StartGossip(messages, clientActorRef) =>
      log.info(s"recvStart ${clientActorRef.actorRef}, ${messages}")
      startGossip(messages, clientActorRef)

    case ConnectionMediator.Ping() =>
      log.info(s"Actor $self received a ping from Connection Mediator")
      connectionMediatorRef = sender()

    case _ =>
      log.info(s"Actor $self received an unexpected message")
  }

}

object GossipManager extends AskPatternConstants {
  def props(dbActorRef: AskableActorRef, monitorRef: ActorRef): Props =
    Props(new GossipManager(dbActorRef, monitorRef))

  def gossipHandler(gossipManager: AskableActorRef, clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.method match
        case MethodType.rumor =>
          gossipManager ? HandleRumor(jsonRpcRequest, clientActorRef)
          Right(jsonRpcRequest)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received a non expected jsonRpcRequest", jsonRpcRequest.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received an unexpected message:" + graphMessage, None))
  }

  def monitorResponse(gossipManager: AskableActorRef, clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcResponse: JsonRpcResponse) =>
      gossipManager ? ManageGossipResponse(jsonRpcResponse, clientActorRef)
      Right(jsonRpcResponse)
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"GossipManager received an unexpected message:$graphMessage while monitoring responses", None))
  }

  def startGossip(gossipManager: AskableActorRef, clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.getParamsMessage match
        case Some(message) =>
          // Start gossiping only if message comes from a real actor (and not from processing pipeline)
          if (clientActorRef != Actor.noSender)
            gossipManager ? StartGossip(Map(jsonRpcRequest.getParamsChannel -> List(message)), clientActorRef)
        case None => /* Do nothing */
      Right(jsonRpcRequest)
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"GossipManager received an unexpected message:$graphMessage while starting gossiping", None))
  }

  sealed trait Event
  final case class HandleRumor(jsonRpcRequest: JsonRpcRequest, clientActorRef: ActorRef)
  final case class ManageGossipResponse(jsonRpcResponse: JsonRpcResponse, clientActorRef: ActorRef)
  final case class StartGossip(messages: Map[Channel, List[Message]], clientActorRef: ActorRef)

  sealed trait GossipManagerMessage
  final case class Ping() extends GossipManagerMessage

}
