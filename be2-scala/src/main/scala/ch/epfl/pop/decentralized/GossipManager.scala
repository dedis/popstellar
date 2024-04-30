package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.decentralized.GossipManager.MonitoredRumor
import ch.epfl.pop.decentralized.{ConnectionMediator, GossipManager}
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadRumors

import scala.concurrent.Await

final case class GossipManager(
    dbActorRef: AskableActorRef,
    monitorRef: ActorRef,
    connectionMediator: AskableActorRef,
    stopProbability: Double = 0.5
) extends Actor with AskPatternConstants {

  private type ServerInfos = (ActorRef, GreetServer)

  monitorRef ! GossipManager.Ping()
  connectionMediator ? ConnectionMediator.Ping()

  private var activeGossipProtocol: Map[Rumor, List[ServerInfos]] = Map.empty

  private def isRumorNew(rumor: Rumor): Boolean = {
    val readRumorDb = dbActorRef ? DbActor.ReadRumors(Map(rumor.senderPk -> List(rumor.rumorId)))
    Await.result(readRumorDb, duration) match
      case DbActorReadRumors(foundRumors) => false
      case failure                        => true
  }

  private def getPeersForRumor(rumor: Rumor): List[ServerInfos] = {
    val activeGossip = activeGossipProtocol.get(rumor)
    activeGossip match
      case Some(peersInfosList) => peersInfosList
      case None                 => List.empty
  }

  private def sendRumorToRandomPeer(jsonRpcRequest: JsonRpcRequest, rumor: Rumor): Unit = {
    // checks the peers to which we already forwarded the message
    val activeGossip: List[ServerInfos] = getPeersForRumor(rumor)
    // selects a random peer from remaining peers
    val randomPeer = connectionMediator ? ConnectionMediator.GetRandomPeer(activeGossip.map(_._1))
    Await.result(randomPeer, duration) match {
      // updates the list based on response
      // if some peers are available we send
      case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
        val alreadySent: List[ServerInfos] = activeGossip :+ (serverRef -> greetServer)
        activeGossipProtocol += (rumor -> alreadySent)
        serverRef ! ClientAnswer(
          Right(jsonRpcRequest)
        )
      // else remove entry
      case ConnectionMediator.NoPeer =>
        activeGossipProtocol = activeGossipProtocol.removed(rumor)
    }
  }

  override def receive: Receive = {
    case GossipManager.SendRumorToRandomPeer(jsonRpcRequest, rumor) =>
      sendRumorToRandomPeer(jsonRpcRequest, rumor)
  }

}

object GossipManager extends AskPatternConstants {
  def props(dbActorRef: AskableActorRef, monitorRef: ActorRef, connectionMediatorRef: AskableActorRef): Props =
    Props(new GossipManager(dbActorRef, monitorRef, connectionMediatorRef))

  def gossipHandler(gossipManager: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.method match
        case MethodType.rumor =>
          val rumor = jsonRpcRequest.getParams.asInstanceOf[Rumor]
          gossipManager ? SendRumorToRandomPeer(jsonRpcRequest, rumor)
          Right(jsonRpcRequest)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received a non expected jsonRpcRequest", jsonRpcRequest.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GossipManager received an unexpected message:" + graphMessage, None))
  }

  sealed trait Event
  final case class MonitoredRumor(jsonRpcRumor: JsonRpcRequest)
  final case class SendRumorToRandomPeer(jsonRpcRequest: JsonRpcRequest, rumor: Rumor)

  sealed trait GossipManagerMessage
  final case class Ping() extends GossipManagerMessage

}
