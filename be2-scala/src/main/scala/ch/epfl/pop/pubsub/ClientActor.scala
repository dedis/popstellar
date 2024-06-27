package ch.epfl.pop.pubsub

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.{AskableActorRef, ask}
import ch.epfl.pop.config.RuntimeEnvironment.serverConf
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.network.method.GreetServer
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.ClientActor.*
import ch.epfl.pop.pubsub.PubSubMediator.*
import ch.epfl.pop.pubsub.graph.{GraphMessage, compactPrinter, prettyPrinter}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

final case class ClientActor(mediator: ActorRef, connectionMediatorRef: ActorRef, isServer: Boolean, initGreet: Boolean) extends Actor with ActorLogging with AskPatternConstants {

  private var wsHandle: Option[ActorRef] = None
  private val subscribedChannels: mutable.Set[Channel] = mutable.Set.empty

  private val mediatorAskable: AskableActorRef = mediator

  private var greetServerSent: Boolean = false

  private def messageWsHandle(event: ClientActorMessage): Unit = {
    event match {
      case ClientAnswer(graphMessage) => wsHandle.fold(())(_ ! graphMessage)
    }
  }

  override def receive: Receive = LoggingReceive {
    case message: ClientActor.Event => message match {
        case ConnectWsHandle(wsClient: ActorRef) =>
          log.info(s"Connecting wsHandle $wsClient to actor ${this.self}")
          wsHandle = Some(wsClient)

          // If server, tell connectionMediator we are online
          if (isServer && initGreet) {
            triggerGreetServer()
          }

        case DisconnectWsHandle =>
          subscribedChannels.foreach(channel => mediator ! PubSubMediator.UnsubscribeFrom(channel, this.self))
          if (isServer) {
            connectionMediatorRef ! ConnectionMediator.ServerLeft(self)
          }

        case ClientActor.SubscribeTo(channel) =>
          val ask: Future[PubSubMediatorMessage] = (mediatorAskable ? PubSubMediator.SubscribeTo(channel, this.self)).map {
            case m: PubSubMediatorMessage => m
          }
          sender() ! Await.result(ask, duration)

        case ClientActor.UnsubscribeFrom(channel) =>
          val ask: Future[PubSubMediatorMessage] = (mediatorAskable ? PubSubMediator.UnsubscribeFrom(channel, this.self)).map {
            case m: PubSubMediatorMessage => m
          }
          sender() ! Await.result(ask, duration)
      }
    case message: PubSubMediatorMessage => message match {
        case SubscribeToAck(channel) =>
          log.info(s"Received ACK from Mediator to subscribe request on channel '$channel'")
          subscribedChannels += channel
        case UnsubscribeFromAck(channel) =>
          log.info(s"Received ACK from Mediator to unsubscribe request on channel '$channel' request")
          subscribedChannels -= channel
        case SubscribeToNAck(channel, reason) =>
          log.info(s"Received NACK from Mediator to subscribe request on channel '$channel'  for reason: $reason")
        case UnsubscribeFromNAck(channel, reason) =>
          log.info(s"Received NACK from Mediator for unsubscribe request on channel '$channel' for reason: $reason")
        case PropagateAck() => // Nothing to do.
      }
    case greetServer: GreetServer =>
      if (!greetServerSent && isServer) {
        triggerGreetServer()
      }
      connectionMediatorRef ! ConnectionMediator.NewServerConnected(self, greetServer)

    case clientAnswer @ ClientAnswer(_) =>
      log.info(s"Sending an answer back to ${if isServer then "server" else "client"} ${wsHandle.get.path.name}: $clientAnswer")
      messageWsHandle(clientAnswer)

    case m @ _ => m match {
        case Failure(exception: Exception) =>
          log.error(">>> Standard Exception : " + m + exception.getMessage)
          exception.printStackTrace()
        case akka.actor.Status.Failure(exception: Exception) =>
          log.error(">>> Actor Exception : " + m + exception.getMessage)
          exception.printStackTrace()
        case Failure(error: Error) =>
          log.error(">>> Error : " + m + error.getMessage)
          error.printStackTrace()
        case akka.actor.Status.Failure(error: Error) =>
          log.error(">>> Actor Error : " + m + error.getMessage)
          error.printStackTrace()
        case _ => log.error("UNKNOWN MESSAGE TO CLIENT ACTOR: " + m)
      }
  }

  /** Build our own GreetServer to identify us to other servers and push it into the websocket
    */
  private def triggerGreetServer(): Unit = {
    val clientAddress = serverConf.externalAddress + s"/${serverConf.clientPath}"
    val serverAddress = serverConf.externalAddress + s"/${serverConf.serverPath}"
    val publicKey = Await.ready(PublishSubscribe.getDbActorRef ? DbActor.ReadServerPublicKey(), duration).value.get match {
      case Success(DbActor.DbActorReadServerPublicKeyAck(pk)) => Some(pk)
      case _                                                  => None
    }

    if (publicKey.isDefined) {
      log.info("Sending greetServer")
      val greetServer = GreetServer(publicKey.get, clientAddress, serverAddress)
      messageWsHandle(ClientAnswer(Right(JsonRpcRequest(
        RpcValidator.JSON_RPC_VERSION,
        MethodType.greet_server,
        greetServer,
        None
      ))))
      greetServerSent = true
    }
  }

}

object ClientActor {
  def props(mediator: ActorRef, connectionMediatorRef: ActorRef, isServer: Boolean, initGreetServer: Boolean): Props =
    Props(new ClientActor(mediator, connectionMediatorRef, isServer, initGreetServer))

  sealed trait ClientActorMessage

  // answer to be sent to the client represented by the client actor
  final case class ClientAnswer(graphMessage: GraphMessage) extends ClientActorMessage {
    override def toString: String = compactPrinter(graphMessage)
  }

  sealed trait Event

  // connect the client actor with the front-end
  final case class ConnectWsHandle(wsClient: ActorRef) extends Event

  // unsubscribe from all channels
  case object DisconnectWsHandle extends Event

  // subscribe to a particular channel
  final case class SubscribeTo(channel: Channel) extends Event

  // unsubscribe from a particular channel
  final case class UnsubscribeFrom(channel: Channel) extends Event

}
