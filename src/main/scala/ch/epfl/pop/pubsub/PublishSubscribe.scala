package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages.{AnswerMessageServer, CreateChannelClient, FetchChannelClient, FetchChannelServer, JsonMessage, NotifyChannelServer, PublishChannelClient, SubscribeChannelClient}
import ch.epfl.pop.pubsub.ChannelActor.{ChannelMessage, CreateMessage, SubscribeMessage}
import org.iq80.leveldb.DB

import scala.util.{Failure, Success, Try}

object PublishSubscribe {
  /**
   * Create a flow that handles JSON messages of a publish-subscribe system.
   *
   * @param pubEntry a mergehub where all published messages are sent
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles JSON messages of a publish-subscribe system
   */
  def jsonFlow(pubEntry: Sink[NotifyChannelServer, NotUsed], actor: ActorRef[ChannelMessage], db : DB)(implicit timeout: Timeout, system: ActorSystem[Nothing]): Flow[JsonMessage, JsonMessage, NotUsed] = {
    val (userSink, userSource) = MergeHub.source[JsonMessage].toMat(BroadcastHub.sink)(Keep.both).run()

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //Initialize Inlet/Outlets
      val input = builder.add(Flow[JsonMessage])
      val merge = builder.add(Merge[JsonMessage](3))
      val output = builder.add(Flow[JsonMessage])

      val Create = 0
      val Subscribe = 0
      val Publish = 1
      val Fetch = 2
      val partitioner = builder.add(Partition[JsonMessage](3,
        {
          _ match {
            case CreateChannelClient(_, _) => Create
            case SubscribeChannelClient(_) => Subscribe
            case PublishChannelClient(_, _) => Publish
            case FetchChannelClient(_,_) => Fetch
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

      val writeDb = Flow[JsonMessage].map {
        case message: PublishChannelClient =>
          //TODO: compute key using hash of event once the specs are updated
          val key = message.event
          val value = message.event
          db.put(key.getBytes(), value.getBytes())
          NotifyChannelServer(message.channel, key)
      }

      val fetchDb = Flow[JsonMessage].map {
        case FetchChannelClient(channel, id) =>
        val event = db.get(id.getBytes())
          if(event == null) {
            AnswerMessageServer(false, Some("Event does not exist"))
          }
          else {
            FetchChannelServer(channel, id, new String(event))
          }
      }

      //Connect the graph
      input ~> partitioner
      partitioner.out(Publish) ~> writeDb ~> pubEntry
      partitioner.out(Create) ~> actorFlow ~> merge
      partitioner.out(Fetch) ~> fetchDb ~> merge
      userSource ~> merge
      merge ~> output

      FlowShape(input.in, output.out)
    }
    )
  }

  /**
   * Create a flow that handles messages of a publish-subscribe system.
   *
   * @param pubEntry a mergehub where all published messages are sent
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles messages of a publish-subscribe system
   */
  def messageFlow(pubEntry: Sink[NotifyChannelServer, NotUsed], actor: ActorRef[ChannelMessage], db : DB)(implicit timeout: Timeout, system: ActorSystem[Nothing]): Flow[Message, Message, NotUsed] = {
    val jFlow = jsonFlow(pubEntry, actor, db : DB)
    Flow.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        val input = builder.add(Flow[Message])
        val output = builder.add(Flow[Message])
        val partitioner = builder.add(Partition[JsonMessage]( 2, {
          case AnswerMessageServer(_,_) => 0
          case _ => 1
        }))
        val merge = builder.add(Merge[JsonMessage](2))


        val parser = Flow[Message].map {
          case TextMessage.Strict(s) => Try(parseMessage(s)) match {
            case Success(message) => message
            case Failure(exception) => AnswerMessageServer(false, Some("Invalid JSON"))
          }
        }
        val formatter = Flow[JsonMessage].map(m => TextMessage.Strict(serializeMessage(m)))

        input ~> parser ~> partitioner
        partitioner.out(0) ~> merge
        partitioner.out(1) ~> jFlow ~> merge
        merge ~> formatter ~> output

        FlowShape(input.in, output.out)
    })
  }
}
