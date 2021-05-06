package ch.epfl.pop.pubsub

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import ch.epfl.pop.model.objects.Channel.Channel
import ch.epfl.pop.pubsub.graph.GraphMessage

import ClientActor._


class ClientActor extends Actor with ActorLogging {

  private var wsHandle: Option[ActorRef] = None

  // called just after actor creation
  // override def preStart(): Unit = mediator ! Subscribe("topic", self) // FIXME topic

  private def messageWsHandle(event: ClientActorMessage): Unit = event match {
    case ClientAnswer(graphMessage) => wsHandle.fold(())(_ ! graphMessage)
  }

  override def receive: Receive = LoggingReceive {
    case message: Event => message match {
      case ConnectWsHandle(wsClient: ActorRef) =>
        log.info(s"Connecting wsHandle $wsClient to actor ${this.self}")
        wsHandle = Some(wsClient)
      case DisconnectWsHandle => ??? // unsubscribe from all
      case SubscribeTo(channel) => ??? // mediator ! Subscribe(channel, self)
      case UnsubscribeFrom(channel) => ??? // mediator ! Unsubscribe(channel, self)
      // case SubscribeToAck(channel) => ???
      // case UnsubscribeFromAck(channel) => ???
    }
    case clientAnswer@ClientAnswer(_) =>
      log.info(s"Sending an answer back to client $wsHandle")
      messageWsHandle(clientAnswer)
    case _ => println("CASE OTHER (should never happen). FIXME remove"); ???
  }
}

object ClientActor {
  def props: Props = Props(new ClientActor())

  sealed trait ClientActorMessage
  // answer to be sent to the client represented by the client actor
  final case class ClientAnswer(graphMessage: GraphMessage) extends ClientActorMessage


  sealed trait Event
  final case class ConnectWsHandle(wsClient: ActorRef) extends Event
  final case object DisconnectWsHandle extends Event
  final case class SubscribeTo(channel: Channel) extends Event
  // final case class SubscribeToAck(channel: Channel) extends Event
  final case class UnsubscribeFrom(channel: Channel) extends Event
  // final case class UnsubscribeFromAck(channel: Channel) extends Event
}

