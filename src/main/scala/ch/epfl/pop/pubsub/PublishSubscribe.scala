package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.FlowShape
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import ch.epfl.pop.json.JsonMessages.{CreateChannelClient, JsonMessage, PublishChannelClient, SubscribeChannelClient}
import ch.epfl.pop.pubsub.ChannelActor.{ChannelMessage, CreateMessage, SubscribeMessage}

object PublishSubscribe {
  /**
   * Create a flow that handles messages of a publish-subscribe system.
   *
   * @param pubEntry a mergehub where all published messages are sent
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles messages of a publish-subscribe system
   */
  def getFlow(pubEntry: Sink[PublishChannelClient, NotUsed], actor: ActorRef[ChannelMessage])(implicit timeout: Timeout, system: ActorSystem[Nothing]): Flow[JsonMessage, JsonMessage, NotUsed] = {
    val (userSink, userSource) = MergeHub.source[JsonMessage].toMat(BroadcastHub.sink)(Keep.both).run()

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //Initialize Inlet/Outlets
      val input = builder.add(Flow[JsonMessage])
      val merge = builder.add(Merge[JsonMessage](2))
      val output = builder.add(Flow[JsonMessage])

      val Create = 0
      val Subscribe = 0
      val Publish = 1
      val partitioner = builder.add(Partition[JsonMessage](2,
        {
          _ match {
            case CreateChannelClient(_, _) => Create
            case SubscribeChannelClient(_) => Subscribe
            case PublishChannelClient(_, _) => Publish
          }
        }
      ))

      //ActorFlow handling channel creation and subscription
      val actorFlow = ActorFlow.ask[JsonMessage, ChannelMessage, JsonMessage](actor) {
        (message, replyTo) =>
          message match {
            case CreateChannelClient(message, contract) => CreateMessage(message, replyTo)
            case SubscribeChannelClient(channel) => SubscribeMessage(channel, userSink, replyTo)
          }
      }

      val convert = Flow[JsonMessage].map {
        case x: PublishChannelClient => x
      }

      //Connect the graph
      input ~> partitioner
      partitioner.out(Publish) ~> convert ~> pubEntry
      partitioner.out(Create) ~> actorFlow ~> merge
      userSource ~> merge
      merge ~> output

      FlowShape(input.in, output.out)
    }
    )
  }
}
