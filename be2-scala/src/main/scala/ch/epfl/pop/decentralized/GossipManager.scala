package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.pattern.AskableActorRef
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.stream.scaladsl.Flow
import ch.epfl.pop.decentralized.GossipManager.TriggerPullState
import ch.epfl.pop.model.network.MethodType.rumor_state
import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType}
import ch.epfl.pop.model.objects.{Channel, PublicKey, RumorData}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorGetRumorStateAck, DbActorReadRumorData, GetRumorState}
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorReadRumorData}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

/** This class is responsible of managing the gossiping of rumors across the network
  * @param dbActorRef
  *   reference to the database actor
  * @param stopProbability
  *   probability with which we stop the gossipping in case of error response
  */
final case class GossipManager(dbActorRef: AskableActorRef, stopProbability: Double = 0.5, pullRate: FiniteDuration = 15.seconds) extends Actor with AskPatternConstants with ActorLogging with Timers {

  private var activeGossipProtocol: Map[JsonRpcRequest, Set[ActorRef]] = Map.empty
  private var rumorMap: Map[PublicKey, Int] = Map.empty
  private var jsonId = 0
  private var publicKey: Option[PublicKey] = None
  private var connectionMediatorRef: AskableActorRef = _

  private val periodicRumorStateKey = 0

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

  /** Does a step of gossipping protocol for given rpc. Tries to find a random peer that hasn't already received this msg If such a peer is found, sends message and updates table accordingly. If no peer is found, ends the protocol.
    * @param rumorRpc
    *   Rpc that must be spreac
    */
  private def updateGossip(rumorRpc: JsonRpcRequest): Unit = {
    // checks the peers to which we already forwarded the message
    val activeGossip: Set[ActorRef] = peersAlreadyReceived(rumorRpc)
    // selects a random peer from remaining peers
    val randomPeer = connectionMediatorRef ? ConnectionMediator.GetRandomPeer(activeGossip)
    Await.result(randomPeer, duration) match {
      // updates the list based on response
      // if some peers are available we send
      case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
        val alreadySent: Set[ActorRef] = activeGossip + serverRef
        activeGossipProtocol += (rumorRpc -> alreadySent)
        log.info(s"rumorSent > dest : ${greetServer.clientAddress}, rumor : $rumorRpc")
        serverRef ! ClientAnswer(
          Right(rumorRpc)
        )
      // else remove entry
      case ConnectionMediator.NoPeer() =>
        activeGossipProtocol = activeGossipProtocol.removed(rumorRpc)
      case _ =>
        log.info(s"Actor $self received an unexpected message waiting for a random peer")
    }
  }

  /** When receiving a rumor that must be relayed, empacks a rumor in a new jsonRPC and tries to do a step of gossipping protocol
    * @param request
    */
  private def handleRumor(request: JsonRpcRequest, serverActorRef: ActorRef): Unit = {
    val rcvRumor = request.getParams.asInstanceOf[Rumor]
    if (isNextRumor(rcvRumor.senderPk, rcvRumor.rumorId))
      val newRumorRequest = prepareRumor(rcvRumor)
      incrementMap(rcvRumor.senderPk)
      activeGossipProtocol += (newRumorRequest -> Set(serverActorRef))
      updateGossip(newRumorRequest)
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

  /** When receives a new publish, empacks messages in a new rumor and starts gossipping it in the network
    * @param messages
    *   Messages to be gossiped by channel
    */
  private def startGossip(messages: Map[Channel, List[Message]]): Unit = {
    if (publicKey.isDefined)
      incrementMap(publicKey.get)
      val rumor: Rumor = Rumor(publicKey.get, rumorMap(publicKey.get), messages)
      val jsonRpcRequest = prepareRumor(rumor)
      val writeRumor = dbActorRef ? DbActor.WriteRumor(rumor)
      Await.result(writeRumor, duration) match
        case DbActorAck() => updateGossip(jsonRpcRequest)
        case _            => log.info(s"Actor (gossip) $self was not able to write rumor in memory. Gossip has not started.")
    else
      log.info(s"Actor (gossip) $self will not be able to start rumors because it has no publicKey")
  }

  private def sendRumorState(): Unit = {
    val randomPeer = connectionMediatorRef ? ConnectionMediator.GetRandomPeer()
    Await.result(randomPeer, duration) match {
      case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
        val rumorStateGet = dbActorRef ? GetRumorState()
        Await.result(rumorStateGet, duration) match
          case DbActorGetRumorStateAck(rumorState) =>
            serverRef ! ClientAnswer(
              Right(JsonRpcRequest(
                RpcValidator.JSON_RPC_VERSION,
                rumor_state,
                rumorState,
                Some(jsonId)
              ))
            )
            jsonId += 1
      case _ =>
        log.info(s"Actor $self received an unexpected message waiting for a random peer")
    }
  }

  private def peersAlreadyReceived(jsonRpcRequest: JsonRpcRequest): Set[ActorRef] = {
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

  override def receive: Receive = {
    case GossipManager.HandleRumor(jsonRpcRequest: JsonRpcRequest, clientActorRef: ActorRef) =>
      handleRumor(jsonRpcRequest, clientActorRef)

    case GossipManager.ManageGossipResponse(jsonRpcResponse) =>
      processResponse(jsonRpcResponse)

    case GossipManager.StartGossip(messages) =>
      startGossip(messages)

    case Monitor.AtLeastOneServerConnected =>
      timers.startTimerWithFixedDelay(periodicRumorStateKey, TriggerPullState, pullRate)

    case Monitor.NoServerConnected =>
      timers.cancel(periodicRumorStateKey)

    case TriggerPullState =>
      sendRumorState()

    case ConnectionMediator.Ping() =>
      log.info(s"Actor $self received a ping from Connection Mediator")
      connectionMediatorRef = sender()

    case _ =>
      log.info(s"Actor $self received an unexpected message")
  }

}

object GossipManager extends AskPatternConstants {
  def props(dbActorRef: AskableActorRef, pullRate: FiniteDuration = 15.seconds): Props =
    Props(new GossipManager(dbActorRef, pullRate = pullRate))

  /** When receiving a rumor, gossip manager handles the rumor by relaying
    *
    * @param gossipManager
    *   reference to the gossip manager of the server
    * @param clientActorRef
    *   reference to the client who sent the message.
    * @return
    */
  def gossipHandler(gossipManager: AskableActorRef, clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.method match
        case MethodType.rumor =>
          gossipManager ? HandleRumor(jsonRpcRequest, clientActorRef)
          Right(jsonRpcRequest)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received a non expected jsonRpcRequest", jsonRpcRequest.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received an unexpected message:" + graphMessage, None))
  }

  /** Monitors responses to check if one is related to a rumor we sent
    * @param gossipManager
    *   reference to the gossip manager of the server
    * @return
    */
  def monitorResponse(gossipManager: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcResponse: JsonRpcResponse) =>
      gossipManager ? ManageGossipResponse(jsonRpcResponse)
      Right(jsonRpcResponse)
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"GossipManager received an unexpected message:$graphMessage while monitoring responses", None))
  }

  /** When a server receives a publish, it starts gossiping
    * @param gossipManager
    *   reference to the gossip manager of the server
    * @param clientActorRef
    *   reference to the client who sent the message. If set to Actor.noSender, should no start gossiping
    * @return
    */
  def startGossip(gossipManager: AskableActorRef, clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.getParamsMessage match
        case Some(message) =>
          // Start gossiping only if message comes from a real actor (and not from processing pipeline)
          if (clientActorRef != Actor.noSender)
            gossipManager ? StartGossip(Map(jsonRpcRequest.getParamsChannel -> List(message)))
        case None => /* Do nothing */
      Right(jsonRpcRequest)
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"GossipManager received an unexpected message:$graphMessage while starting gossiping", None))
  }

  sealed trait Event
  final case class HandleRumor(jsonRpcRequest: JsonRpcRequest, clientActorRef: ActorRef)
  final case class ManageGossipResponse(jsonRpcResponse: JsonRpcResponse)
  final case class StartGossip(messages: Map[Channel, List[Message]])
  final case class TriggerPullState()

  sealed trait GossipManagerMessage
  final case class Ping() extends GossipManagerMessage

}
