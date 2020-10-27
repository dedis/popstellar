package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import ch.epfl.pop.json.JsonMessages.{AnswerMessageServer, JsonMessage, PublishChannelClient}

object ChannelActor {

  sealed trait ChannelMessage

  final case class CreateMessage(channel: String, replyTo: ActorRef[JsonMessage]) extends ChannelMessage

  final case class SubscribeMessage(channel: String, out: Sink[JsonMessage, NotUsed], replyTo: ActorRef[JsonMessage])
    extends ChannelMessage

  /**
   * Create an actor handling channel creation and subscription
   *
   * @param publishExit a source emitting all published messages
   * @return an actor handling channel creation and subscription
   */
  def apply(publishExit: Source[PublishChannelClient, NotUsed]): Behavior[ChannelMessage] = channelHandler(Map.empty, publishExit)

  private def channelHandler(m: Map[String, Source[PublishChannelClient, NotUsed]],
                             publishExit: Source[PublishChannelClient, NotUsed]): Behavior[ChannelMessage] = {
    Behaviors.receive { (ctx, message) =>
      implicit val system = ctx.system
      message match {

        case CreateMessage(channel, replyTo) =>
          if (!m.contains(channel)) {
            val (entry, exit) = MergeHub.source[PublishChannelClient].toMat(BroadcastHub.sink)(Keep.both).run()

            publishExit.filter(_.channel == channel).runWith(entry)
            replyTo ! AnswerMessageServer(true, None)
            channelHandler(m + (channel -> exit), publishExit)
          }
          else {
            replyTo ! AnswerMessageServer(false, Some("The channel already exist"))
            Behaviors.same
          }

        case SubscribeMessage(channel, out, replyTo) =>
          if (m.contains(channel)) {
            val channelSource = m(channel)
            channelSource.runWith(out)
            replyTo ! AnswerMessageServer(true, None)
          }
          else {
            replyTo ! AnswerMessageServer(false, Some("Unknown channel."))
          }
          Behaviors.same
      }
    }
  }
}
