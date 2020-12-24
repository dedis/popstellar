package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import ch.epfl.pop.json.JsonMessages.{AnswerErrorMessageServer, AnswerResultIntMessageServer, JsonMessageAnswerServer, PropagateMessageServer}
import ch.epfl.pop.json.MessageErrorContent

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
  final case class CreateMessage(channel: String, replyTo: ActorRef[Boolean]) extends ChannelMessage

  /**
   * Request to subscribe to a channel
   * @param channel the channel to subscribe
   * @param out the entry of the client stream
   * @param id the id of the request
   * @param replyTo the actor to reply to once the subscription is done
   */
  final case class SubscribeMessage(channel: String, out: Sink[PropagateMessageServer, NotUsed], id: Int, replyTo: ActorRef[ChannelActorAnswer])
    extends ChannelMessage



  sealed trait ChannelActorAnswer

  //final case class AnswerCreate(jsonMessage: JsonMessageAnswerServer) extends ChannelActorAnswer

  final case class AnswerSubscribe(jsonMessage: JsonMessageAnswerServer, channel : String, killSwitch: Option[UniqueKillSwitch]) extends ChannelActorAnswer with UnsubMessage


  /**
   * Create an actor handling channel creation and subscription
   *
   * @param publishExit a source emitting all published messages
   * @return an actor handling channel creation and subscription
   */

  def apply(publishExit: Source[PropagateMessageServer, NotUsed])(implicit system :ActorSystem[Nothing]): Behavior[ChannelMessage] = {
    //Setup root channel
    val root = "/root"
    val (entry, exit) = MergeHub.source[PropagateMessageServer].toMat(BroadcastHub.sink)(Keep.both).run()
    publishExit.filter(_.params.channel == root).runWith(entry)

    channelHandler(Map(root -> exit), publishExit)
  }


  private def channelHandler(channelsOutputs: Map[String, Source[PropagateMessageServer, NotUsed]],
                             publishExit: Source[PropagateMessageServer, NotUsed]): Behavior[ChannelMessage] = {
    Behaviors.receive { (ctx, message) =>
      implicit val system: ActorSystem[Nothing] = ctx.system
      message match {

        case CreateMessage(channel,  replyTo) =>
          if (!channelsOutputs.contains(channel)) {
            ctx.log.debug("creating channel: " + channel)
            val (entry, exit) = MergeHub.source[PropagateMessageServer].toMat(BroadcastHub.sink)(Keep.both).run()

            publishExit.filter(_.params.channel == channel).runWith(entry)
            ctx.log.debug("New channel map: " + (channelsOutputs + (channel -> exit)).toString())
            replyTo ! true
            channelHandler(channelsOutputs + (channel -> exit), publishExit)

          }
          else {
            replyTo ! false
            Behaviors.same
          }



        case SubscribeMessage(channel, out, id, replyTo) =>
          if (channelsOutputs.contains(channel)) {
            val channelSource = channelsOutputs(channel)
            val killSwitch = channelSource
              .viaMat(KillSwitches.single)(Keep.right)
              .toMat(out)(Keep.left)
              .run()
            replyTo ! AnswerSubscribe(AnswerResultIntMessageServer(id = id), channel, Some(killSwitch))
          }
          else {
            ctx.log.debug(channelsOutputs.toString())
            val error = MessageErrorContent(-2, "Invalid resource: channel " + channel + " does not exist.")
            replyTo ! AnswerSubscribe(AnswerErrorMessageServer(error = error, id = Some(id)), channel, None)
          }
          Behaviors.same
      }
    }
  }
}
