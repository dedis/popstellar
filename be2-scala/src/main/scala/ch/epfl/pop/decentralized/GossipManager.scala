package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.slf4j.Logger
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.decentralized.{ConnectionMediator, GossipManager}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType}
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadRumor

import scala.concurrent.Await
import scala.util.{Random, Success}

final case class GossipManager(
    dbActorRef: AskableActorRef,
    monitorRef: ActorRef,
    connectionMediator: AskableActorRef,
    stopProbability: Double = 0.5
) extends Actor with AskPatternConstants with ActorLogging {

  private type ServerInfos = (ActorRef, GreetServer)
  private val publicKey: Option[PublicKey] = {
    val readPk = dbActorRef ? DbActor.ReadServerPublicKey()
    Await.result(readPk, duration) match
      case DbActor.DbActorReadServerPublicKeyAck(pk) => Some(pk)
      case _                                         => None
  }
  private var rumorId = 0
  private var jsonId = 0

  monitorRef ! GossipManager.Ping()
  connectionMediator ? GossipManager.Ping()

  private var activeGossipProtocol: Map[JsonRpcRequest, List[ServerInfos]] = Map.empty

  private def isRumorNew(rumor: Rumor): Boolean = {
    val readRumorDb = dbActorRef ? DbActor.ReadRumor(rumor.senderPk -> rumor.rumorId)
    Await.result(readRumorDb, duration) match
      case DbActorReadRumor(foundRumors) =>
        foundRumors.isEmpty
  }

  private def getPeersForRumor(jsonRpcRequest: JsonRpcRequest): List[ServerInfos] = {
    val activeGossip = activeGossipProtocol.get(jsonRpcRequest)
    activeGossip match
      case Some(peersInfosList) => peersInfosList
      case None                 => List.empty
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

  private def updateGossip(rumorRpc: JsonRpcRequest): Boolean = {
    // checks the peers to which we already forwarded the message
    val activeGossip: List[ServerInfos] = getPeersForRumor(rumorRpc)
    // get senderpk to avoid sending rumor back
    val senderPk: PublicKey = rumorRpc.getParams.asInstanceOf[Rumor].senderPk
    // selects a random peer from remaining peers
    val randomPeer = connectionMediator ? ConnectionMediator.GetRandomPeer(activeGossip.map(_._2.publicKey).appended(senderPk))
    Await.result(randomPeer, duration) match {
      // updates the list based on response
      // if some peers are available we send
      case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
        val alreadySent: List[ServerInfos] = activeGossip :+ (serverRef -> greetServer)
        activeGossipProtocol += (rumorRpc -> alreadySent)
        log.info(s"rumorSent > dest : ${greetServer.clientAddress}, rumor : $rumorRpc")
        serverRef ! ClientAnswer(
          Right(rumorRpc)
        )
        true
      // else remove entry
      case ConnectionMediator.NoPeer() =>
        activeGossipProtocol = activeGossipProtocol.removed(rumorRpc)
        false
      case _ =>
        log.info(s"Actor $self received an unexpected message waiting for a random peer")
        false
    }
  }

  private def handleRumor(request: JsonRpcRequest): Unit = {
    val rcvRumor = request.getParams.asInstanceOf[Rumor]
    val newRumorRequest = prepareRumor(rcvRumor)
    updateGossip(newRumorRequest)

  }

  private def processResponse(response: JsonRpcResponse): Unit = {
    val activeGossipPeers = activeGossipProtocol.filter((k, _) => k.id == response.id)

    // response is expected because only one entry exists
    if (activeGossipPeers.size == 1) {
      activeGossipPeers.foreach { (rumorRpc, _) =>
        if (response.result.isEmpty && Random.nextDouble() < stopProbability) {
          activeGossipProtocol -= rumorRpc
        } else {
          updateGossip(rumorRpc)
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

  private def startGossip(messages: Map[Channel, List[Message]]): Unit = {
    val rumor: Rumor = Rumor(publicKey.get, rumorId, messages)
    val jsonRpcRequest = prepareRumor(rumor)
    rumorId += 1
    updateGossip(jsonRpcRequest)
  }

  override def receive: Receive = {
    case GossipManager.HandleRumor(jsonRpcRequest: JsonRpcRequest) =>
      handleRumor(jsonRpcRequest)

    case GossipManager.ManageGossipResponse(jsonRpcResponse) =>
      processResponse(jsonRpcResponse)

    case GossipManager.StartGossip(messages) =>
      startGossip(messages)

    case _ =>
      log.info(s"Actor $self received an unexpected message")
  }

}

object GossipManager extends AskPatternConstants {
  def props(dbActorRef: AskableActorRef, monitorRef: ActorRef, connectionMediatorRef: AskableActorRef): Props =
    Props(new GossipManager(dbActorRef, monitorRef, connectionMediatorRef))

  def gossipHandler(gossipManager: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.method match
        case MethodType.rumor =>
          gossipManager ? HandleRumor(jsonRpcRequest)
          Right(jsonRpcRequest)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received a non expected jsonRpcRequest", jsonRpcRequest.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received an unexpected message:" + graphMessage, None))
  }

  def monitorResponse(gossipManager: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcResponse: JsonRpcResponse) =>
      gossipManager ? ManageGossipResponse(jsonRpcResponse)
      Right(jsonRpcResponse)
    case graphMessage @ _ => graphMessage
  }

  def startGossip(gossipManager: AskableActorRef, actorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.getParamsMessage match
        case Some(message) =>
          if (actorRef != Actor.noSender)
            gossipManager ? StartGossip(Map(jsonRpcRequest.getParamsChannel -> List(message)))
        case None => /* Do nothing */
      Right(jsonRpcRequest)
    case graphMessage @ _ => graphMessage
  }

  sealed trait Event
  final case class HandleRumor(jsonRpcRequest: JsonRpcRequest)
  final case class ManageGossipResponse(jsonRpcResponse: JsonRpcResponse)
  final case class StartGossip(messages: Map[Channel, List[Message]])

  sealed trait GossipManagerMessage
  final case class Ping() extends GossipManagerMessage

}
