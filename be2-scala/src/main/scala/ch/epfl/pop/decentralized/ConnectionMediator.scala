package ch.epfl.pop.decentralized

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.pattern.AskableActorRef

import ch.epfl.pop.model.network.method.{Heartbeat, ParamsWithMap}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PublishSubscribe}
final case class ConnectionMediator(
    monitorRef: ActorRef,
    mediatorRef: ActorRef,
    dbActorRef: AskableActorRef,
    messageRegistry: MessageRegistry
) extends Actor with ActorLogging with AskPatternConstants {
  implicit val system: ActorSystem = ActorSystem()

  // List of servers connected
  private var serverSet: Set[ActorRef] = Set()

  override def receive: Receive = {

    // Connect to some servers
    case ConnectionMediator.ConnectTo(urlList) =>
      urlList.map(url =>
        Http().singleWebSocketRequest(
          WebSocketRequest(url),
          PublishSubscribe.buildGraph(
            mediatorRef,
            dbActorRef,
            messageRegistry,
            self,
            isServer = true
          )
        )
      )

    case ConnectionMediator.NewServerConnected(serverRef) =>
      log.info("Received new server")
      if (serverSet.isEmpty)
        monitorRef ! Monitor.AtLeastOneServerConnected

      serverSet += serverRef

    case ConnectionMediator.ServerLeft(serverRef) =>
      log.info("Server left")
      serverSet -= serverRef
      // Tell monitor to stop scheduling heartbeats since there is no one to receive them
      if (serverSet.isEmpty)
        monitorRef ! Monitor.NoServerConnected

    case Heartbeat(map) =>
      log.info("Sending a heartbeat to the servers")
      serverSet.map(server =>
        server ! ClientAnswer(
          Right(JsonRpcRequest(
            RpcValidator.JSON_RPC_VERSION,
            MethodType.HEARTBEAT,
            new ParamsWithMap(map),
            None
          ))
        )
      )
  }
}

object ConnectionMediator {

  def props(monitorRef: ActorRef, mediatorRef: ActorRef, dbActorRef: AskableActorRef, messageRegistry: MessageRegistry): Props =
    Props(new ConnectionMediator(monitorRef, mediatorRef, dbActorRef, messageRegistry))

  sealed trait Event
  final case class ConnectTo(urlList: List[String]) extends Event
  final case class NewServerConnected(serverRef: ActorRef) extends Event
  final case class ServerLeft(serverRef: ActorRef) extends Event
}
