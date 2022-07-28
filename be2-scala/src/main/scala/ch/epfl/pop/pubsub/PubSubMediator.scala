package ch.epfl.pop.pubsub

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.Broadcast
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.PubSubMediator._
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Success

class PubSubMediator extends Actor with ActorLogging with AskPatternConstants {

  // Map from channels to the collection of actors subscribed to the channel
  private var channelMap: mutable.Map[Channel, mutable.Set[ActorRef]] = mutable.Map.empty
  lazy val dbActor: AskableActorRef = DbActor.getInstance

  private def subscribeTo(channel: Channel, clientActorRef: ActorRef): Future[PubSubMediatorMessage] = {
    val askChannelExistence = dbActor ? DbActor.ChannelExists(channel)
    askChannelExistence.transformWith {
      // if the channel exists in db, we can start thinking about subscribing a client to it
      case Success(_) => channelMap.get(channel) match {
          // if we have people already subscribed to said channel
          case Some(set) =>
            if (set.contains(clientActorRef)) {
              log.info(s"$clientActorRef already subscribed to '$channel'")
              Future(SubscribeToAck(channel))
            } else {
              log.info(s"Subscribing $clientActorRef to channel '$channel'")
              set += clientActorRef
              Future(SubscribeToAck(channel))
            }

          // if we have no one subscribed to said channel
          case _ =>
            log.info(s"Subscribing $clientActorRef to channel '$channel'")
            channelMap = channelMap ++ List(channel -> mutable.Set(clientActorRef))
            Future(SubscribeToAck(channel))
        }

      // db doesn't recognize the channel, thus mediator cannot subscribe anyone to a non existing channel
      case _ =>
        val reason: String = s"Channel '$channel' doesn't exist in db"
        log.info(reason)
        Future(SubscribeToNAck(channel, reason))
    }
  }

  private def unsubscribeFrom(channel: Channel, clientActorRef: ActorRef): PubSubMediatorMessage = channelMap.get(channel) match {
    case Some(set) if set.contains(clientActorRef) =>
      log.info(s"Unsubscribing $clientActorRef from channel '$channel'")
      set -= clientActorRef
      UnsubscribeFromAck(channel)
    case Some(_) =>
      val reason: String = s"Actor $clientActorRef is not subscribed to channel '$channel'"
      log.info(reason)
      UnsubscribeFromNAck(channel, reason)
    case _ =>
      val reason: String = s"Channel '$channel' does not exist in the system"
      log.info(reason)
      UnsubscribeFromNAck(channel, reason)
  }

  private def broadcast(channel: Channel, message: Message): Unit = {

    def generateAnswer(): GraphMessage = {
      val broadcast: Broadcast = Broadcast(channel, message)
      val answer: JsonRpcRequest = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.BROADCAST, broadcast, None)
      Left(answer)
    }

    channelMap.get(channel) match {
      case Some(clientRefs: mutable.Set[ActorRef]) =>
        log.info(s"Actor $self (PubSubMediator) is propagating a message to ${clientRefs.size} clients")
        clientRefs.foreach(clientRef => clientRef ! ClientActor.ClientAnswer(generateAnswer()))

      case _ =>
        log.info(s"Actor $self (PubSubMediator) did not propagate any message since no client is subscribed to channel $channel")
    }
  }

  override def receive: Receive = LoggingReceive {

    case PubSubMediator.SubscribeTo(channel, clientActorRef) =>
      val senderRef = sender() // since we are asking dbActor, sender (client) could get overwritten!
      val ask: Future[PubSubMediatorMessage] = subscribeTo(channel, clientActorRef)
      senderRef ! Await.result(ask, duration)

    case PubSubMediator.UnsubscribeFrom(channel, clientActorRef) =>
      sender() ! unsubscribeFrom(channel, clientActorRef)

    case PubSubMediator.Propagate(channel, message) => broadcast(channel, message)

    case m @ _ =>
      log.error(s"PubSubMediator received an unknown message : $m")
  }
}

object PubSubMediator {
  def props: Props = Props(new PubSubMediator())

  // PubSubMediator Events correspond to messages the actor may receive
  sealed trait Event

  // subscribe a client to a particular channel
  final case class SubscribeTo(channel: Channel, clientActorRef: ActorRef) extends Event

  // unsubscribe a client from a particular channel
  final case class UnsubscribeFrom(channel: Channel, clientActorRef: ActorRef) extends Event

  // propagate a message to clients subscribed to a particular channel
  final case class Propagate(channel: Channel, message: Message) extends Event

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
