package ch.epfl.pop.pubsub.graph

import akka.actor.typed.ActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, Hash}

// FIXME will remove DBActor once everything is working
object DbActorNew {

  sealed trait DbMessage

  /**
   * Request to write a message in the database
   * @param channel the channel where the message must be published
   * @param message the message to write in the database
   * @param replyTo the actor to respond to
   */
  final case class Write(channel: Channel.Channel, message: Message, replyTo: ActorRef[Boolean]) extends DbMessage

  /**
   * Request to read a specific messages on a channel
   * @param channel the channel where the message was published
   * @param id the id of the message we want to read
   * @param replyTo the actor to reply to
   */
  final case class Read(channel: Channel.Channel, id: Hash, replyTo: ActorRef[Option[Message]]) extends DbMessage
}
