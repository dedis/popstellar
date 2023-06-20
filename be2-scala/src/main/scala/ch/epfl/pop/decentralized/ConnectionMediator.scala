package ch.epfl.pop.decentralized

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.pattern.AskableActorRef
import ch.epfl.pop.decentralized.ConnectionMediator.{NewServerConnected, ReadPeersClientAddress, ReadPeersClientAddressAck}
import ch.epfl.pop.model.network.method.{GreetServer, Heartbeat, ParamsWithMap}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PublishSubscribe}

import scala.collection.immutable.HashMap
final case class ConnectionMediator(
    monitorRef: ActorRef,
    mediatorRef: ActorRef,
    dbActorRef: AskableActorRef,
    messageRegistry: MessageRegistry
) extends Actor with ActorLogging with AskPatternConstants {
  implicit val system: ActorSystem = ActorSystem()

  private var serverMap: HashMap[ActorRef, GreetServer] = HashMap()

  // Ping Monitor to inform it of our ActorRef
  monitorRef ! ConnectionMediator.Ping()

  override def receive: Receive = {

    // Connect to some servers
    case ConnectionMediator.ConnectTo(urlList) =>
      val urlDiff = urlList.toSet.diff(serverMap.values.map(g => g.serverAddress).toSet)
      urlDiff.map(url =>
        Http().singleWebSocketRequest(
          WebSocketRequest(url),
          PublishSubscribe.buildGraph(
            mediatorRef,
            dbActorRef,
            messageRegistry,
            monitorRef,
            self,
            isServer = true,
            initGreetServer = true
          )
        )
      )

    case ConnectionMediator.ServerLeft(serverRef) =>
      log.info("Server left")
      serverMap -= serverRef
      // Tell monitor to stop scheduling heartbeats since there is no one to receive them
      if (serverMap.isEmpty)
        monitorRef ! Monitor.NoServerConnected

    case ConnectionMediator.ReadPeersClientAddress() =>
      if (serverMap.isEmpty)
        sender() ! ConnectionMediator.ReadPeersClientAddressAck(List.empty[String])
      else
        sender() ! ConnectionMediator.ReadPeersClientAddressAck(serverMap.values.map(gr => gr.clientAddress).toList)

    case ConnectionMediator.NewServerConnected(serverRef, greetServer) =>
      if (serverMap.isEmpty) {
        monitorRef ! Monitor.AtLeastOneServerConnected
      }
      serverMap += ((serverRef, greetServer))

    case Heartbeat(map) =>
      log.info("Sending a heartbeat to the servers")
      serverMap.keys.map(server =>
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
  final case class NewServerConnected(serverRef: ActorRef, greetServer: GreetServer) extends Event
  final case class ServerLeft(serverRef: ActorRef) extends Event
  final case class Ping() extends Event
  final case class ReadPeersClientAddress() extends Event

  sealed trait ConnectionMediatorMessage
  final case class ReadPeersClientAddressAck(list: List[String]) extends ConnectionMediatorMessage
}
