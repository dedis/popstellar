package ch.epfl.pop.decentralized

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskableActorRef
import ch.epfl.pop.decentralized.GossipManager.MonitoredRumor
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer

import scala.::
import scala.concurrent.Await

final case class GossipManager(
    dbActorRef: AskableActorRef,
    monitorRef: ActorRef,
    connectionMediator: AskableActorRef,
    stopProbability: Double
) extends Actor with AskPatternConstants {

  private type ServerInfos = (ActorRef, GreetServer)

  monitorRef ! GossipManager.Ping()

  private var activeGossipProtocol: Map[Rumor, List[ServerInfos]] = Map.empty

  override def receive: Receive = {
    case GossipManager.MonitoredRumor(jsonRpcRumor) =>
      val rumor = jsonRpcRumor.getParams.asInstanceOf[Rumor]

      val activeGossip = activeGossipProtocol.get(rumor)
      val excludedPeers : List[ActorRef] = activeGossip match {
        case Some(peersInfosList) => peersInfosList.map(_._1)
        case None => List.empty
      }

      val randomPeer = connectionMediator ? ConnectionMediator.GetRandomPeer(excludedPeers)
      Await.result(randomPeer, duration) match {
        case ConnectionMediator.GetRandomPeerAck(serverRef, greetServer) =>
          val alreadySent: List[ServerInfos] = activeGossip match
            case Some(peers) => peers :+ (serverRef -> greetServer)
            case None => List(serverRef -> greetServer)
          activeGossipProtocol += (rumor -> alreadySent)
          serverRef ! ClientAnswer(
            Right(jsonRpcRumor)
          )
      }

  }

}

object GossipManager {
  def props(dbActorRef: AskableActorRef, monitorRef: ActorRef, connectionMediator: AskableActorRef ,stopProbability: Double = 0.5): Props =
    Props(GossipManager(dbActorRef, monitorRef, connectionMediator, stopProbability))

  sealed trait Event
  final case class MonitoredRumor(jsonRpcRumor: JsonRpcRequest)

  sealed trait GossipManagerMessage
  final case class Ping() extends GossipManagerMessage
}
