package ch.epfl.pop.pubsub

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.ClientActor._
import ch.epfl.pop.pubsub.PubSubMediator._
import ch.epfl.pop.pubsub.graph.GraphMessage

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Failure

final case class ClientActor(mediator: ActorRef) extends Actor with ActorLogging with AskPatternConstants {

  private var wsHandle: Option[ActorRef] = None
  private val subscribedChannels: mutable.Set[Channel] = mutable.Set.empty

  private val mediatorAskable: AskableActorRef = mediator

  private def messageWsHandle(event: ClientActorMessage): Unit = event match {
    case ClientAnswer(graphMessage) => wsHandle.fold(())(_ ! graphMessage)
  }

  override def receive: Receive = LoggingReceive {
    case message: ClientActor.Event => message match {
        case ConnectWsHandle(wsClient: ActorRef) =>
          log.info(s"Connecting wsHandle $wsClient to actor ${this.self}")
          wsHandle = Some(wsClient)

        case DisconnectWsHandle => subscribedChannels.foreach(channel => mediator ! PubSubMediator.UnsubscribeFrom(channel, this.self))

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
          log.info(s"Actor $self received ACK mediator $mediator for the subscribe to channel '$channel' request")
          subscribedChannels += channel
        case UnsubscribeFromAck(channel) =>
          log.info(s"Actor $self received ACK mediator $mediator for the unsubscribe from channel '$channel' request")
          subscribedChannels -= channel
        case SubscribeToNAck(channel, reason) =>
          log.info(s"Actor $self received NACK mediator $mediator for the subscribe to channel '$channel' request for reason: $reason")
        case UnsubscribeFromNAck(channel, reason) =>
          log.info(s"Actor $self received NACK mediator $mediator for the unsubscribe from channel '$channel' request for reason: $reason")
      }
    case clientAnswer @ ClientAnswer(_) =>
      log.info(s"Sending an answer back to client $wsHandle: $clientAnswer")
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
}

object ClientActor {
  def props(mediator: ActorRef): Props = Props(new ClientActor(mediator))

  sealed trait ClientActorMessage

  // answer to be sent to the client represented by the client actor
  final case class ClientAnswer(graphMessage: GraphMessage) extends ClientActorMessage

  sealed trait Event

  // connect the client actor with the front-end
  final case class ConnectWsHandle(wsClient: ActorRef) extends Event

  // unsubscribe from all channels
  final case object DisconnectWsHandle extends Event

  // subscribe to a particular channel
  final case class SubscribeTo(channel: Channel) extends Event

  // unsubscribe from a particular channel
  final case class UnsubscribeFrom(channel: Channel) extends Event

}
