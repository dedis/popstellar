package ch.epfl.pop.pubsub

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.ClientActor.{SubscribeTo, UnsubscribeFrom}
import ch.epfl.pop.pubsub.PubSubMediator.{SubscribeToAck, SubscribeToNAck, UnsubscribeFromAck, UnsubscribeFromNAck}
import ch.epfl.pop.pubsub.graph.DbActor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class PubSubMediator(val dbActor: AskableActorRef) extends Actor with ActorLogging with AskPatternConstants {

  // Map from channels to the collection of actors subscribed to the channel
  private var channelMap: mutable.Map[Channel, mutable.Set[ActorRef]] = mutable.Map.empty

  override def receive: Receive = LoggingReceive {

    case SubscribeTo(channel) =>
      val senderRef = sender // since we are asking dbActor, sender (client) will get overwritten!
      (dbActor ? DbActor.ChannelExists(channel)).map {
        case true => channelMap.get(channel) match {
          case Some(set) =>
            if (set.contains(senderRef)) {
              val reason: String = s"Actor $senderRef was already subscribed to channel '$channel'"
              log.info(reason)
              senderRef ! SubscribeToNAck(channel, reason)
            } else {
              log.info(s"Subscribing $senderRef to channel '$channel'")
              set += senderRef
              senderRef ! SubscribeToAck(channel)
            }
          case _ =>
            log.info(s"Subscribing $senderRef to channel '$channel'")
            channelMap = channelMap + (channel -> mutable.Set(senderRef))
            senderRef ! SubscribeToAck(channel)
        }
        case _ =>
          val reason: String = s"Channel '$channel' doesn't exist in db"
          log.info(reason)
          senderRef ! SubscribeToNAck(channel, reason)
      }

    case UnsubscribeFrom(channel) => channelMap.get(channel) match {
      case Some(set) if set.contains(sender) =>
        log.info(s"Unsubscribing $sender from channel '$channel'")
        set -= sender
        sender ! UnsubscribeFromAck(channel)
      case Some(_) =>
        sender ! UnsubscribeFromNAck(channel, s"Actor $sender is not subscribed to channel '$channel'")
      case _ =>
        sender ! UnsubscribeFromNAck(channel, s"Channel '$channel' does not exist in the system")
    }
  }
}

object PubSubMediator {
  def props(dbActorRef: AskableActorRef): Props = Props(new PubSubMediator(dbActorRef))

  sealed trait PubSubMediatorMessage
  // subscribe confirmation (sender successfully subscribed to channel channel)
  final case class SubscribeToAck(channel: Channel) extends PubSubMediatorMessage
  // subscribe confirmation (sender successfully unsubscribed from channel channel)
  final case class UnsubscribeFromAck(channel: Channel) extends PubSubMediatorMessage
  // unsubscribe infirmation (sender failed to subscribe to channel channel)
  final case class SubscribeToNAck(channel: Channel, reason: String) extends PubSubMediatorMessage
  // unsubscribe infirmation (sender failed to unsubscribe from channel channel)
  final case class UnsubscribeFromNAck(channel: Channel, reason: String) extends PubSubMediatorMessage
}
