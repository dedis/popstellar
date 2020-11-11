package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import ch.epfl.pop.json.JsonMessages.{AnswerMessageServer, JsonMessage, NotifyChannelServer, PublishChannelClient}

/**
 * The role of the ChannelActor is to handle the creation of channels and subscribe requests of clients.
 */
object ChannelActor {

  /**
   * Requests to the ChannelActor
   */
  sealed trait ChannelMessage

  /**
   * Request to create a channel
   * @param channel the channel name
   * @param replyTo the actor to reply to once the creation is done
   */
  final case class CreateMessage(channel: String, replyTo: ActorRef[ChannelActorAnswer]) extends ChannelMessage

  /**
   * Request to subsccribe to a channel
   * @param channel the channel to subscribe
   * @param out the entry of the client stream
   * @param replyTo the actor to reply to once the subscription is done
   */
  final case class SubscribeMessage(channel: String, out: Sink[JsonMessage, NotUsed], replyTo: ActorRef[ChannelActorAnswer])
    extends ChannelMessage



  sealed trait ChannelActorAnswer

  final case class Answer(jsonMessage: JsonMessage) extends ChannelActorAnswer

  final case class AnswerSubscribe(jsonMessage: JsonMessage, channel : String, killSwitch: Option[UniqueKillSwitch]) extends ChannelActorAnswer with UnsubMessage


  /**
   * Create an actor handling channel creation and subscription
   *
   * @param publishExit a source emitting all published messages
   * @return an actor handling channel creation and subscription
   */
  def apply(publishExit: Source[NotifyChannelServer, NotUsed]): Behavior[ChannelMessage] = channelHandler(Map.empty, publishExit)

  private def channelHandler(channelsOutputs: Map[String, Source[NotifyChannelServer, NotUsed]],
                             publishExit: Source[NotifyChannelServer, NotUsed]): Behavior[ChannelMessage] = {
    Behaviors.receive { (ctx, message) =>
      implicit val system = ctx.system
      message match {

        case CreateMessage(channel, replyTo) =>
          if (!channelsOutputs.contains(channel)) {
            val (entry, exit) = MergeHub.source[NotifyChannelServer].toMat(BroadcastHub.sink)(Keep.both).run()

            publishExit.filter(_.channel == channel).runWith(entry)
            replyTo ! Answer(AnswerMessageServer(true, None))
            channelHandler(channelsOutputs + (channel -> exit), publishExit)
          }
          else {
            replyTo ! Answer(AnswerMessageServer(false, Some("The channel already exist")))
            Behaviors.same
          }

        case SubscribeMessage(channel, out, replyTo) =>
          if (channelsOutputs.contains(channel)) {
            val channelSource = channelsOutputs(channel)
            val killSwitch = channelSource
              .viaMat(KillSwitches.single)(Keep.right)
              .toMat(out)(Keep.left)
              .run()
            replyTo ! AnswerSubscribe(AnswerMessageServer(true, None), channel, Some(killSwitch))
          }
          else {
            replyTo ! AnswerSubscribe(AnswerMessageServer(false, Some("Unknown channel.")), channel, None)
          }
          Behaviors.same
      }
    }
  }
}
