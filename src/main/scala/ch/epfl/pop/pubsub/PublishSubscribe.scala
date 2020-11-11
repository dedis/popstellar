package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{FlowShape, UniqueKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import ch.epfl.pop.DBActor.{DBMessage, Read, Write}
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages.{AnswerMessageServer, CreateChannelClient, FetchChannelClient, FetchChannelServer, JsonMessage, NotifyChannelServer, PublishChannelClient, SubscribeChannelClient, UnsubscribeChannelClient}
import ch.epfl.pop.pubsub.ChannelActor.{Answer, AnswerSubscribe, ChannelActorAnswer, ChannelMessage, CreateMessage, SubscribeMessage}


import scala.util.{Failure, Success, Try}

object PublishSubscribe {
  /**
   * Create a flow that handles JSON messages of a publish-subscribe system.
   *
   * @param pubEntry a mergehub where all published messages are sent
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles JSON messages of a publish-subscribe system
   */
  def jsonFlow(pubEntry: Sink[NotifyChannelServer, NotUsed], actor: ActorRef[ChannelMessage], dbActor : ActorRef[DBMessage])
              (implicit timeout: Timeout, system: ActorSystem[Nothing]): Flow[JsonMessage, JsonMessage, NotUsed] = {
    val (userSink, userSource) = MergeHub.source[JsonMessage].toMat(BroadcastHub.sink)(Keep.both).run()

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //Initialize Inlet/Outlets
      val input = builder.add(Flow[JsonMessage])
      val merge = builder.add(Merge[JsonMessage](4))
      val output = builder.add(Flow[JsonMessage])
      val mergeUnsub = builder.add(Merge[UnsubMessage](2))

      val Create = 0
      val Subscribe = 0
      val Unsubscribe = 3
      val Publish = 1
      val Fetch = 2
      val partitioner = builder.add(Partition[JsonMessage](4,
        {
          _ match {
            case CreateChannelClient(_, _) => Create
            case SubscribeChannelClient(_) => Subscribe
            case UnsubscribeChannelClient(_) => Unsubscribe
            case PublishChannelClient(_, _) => Publish
            case FetchChannelClient(_,_) => Fetch
          }
        }
      ))

      //ActorFlow handling channel creation and subscription
      val actorFlow = ActorFlow.ask[JsonMessage, ChannelMessage, ChannelActorAnswer](actor) {
        (message, replyTo) =>
          message match {
            case CreateChannelClient(message, contract) => CreateMessage(message, replyTo)
            case SubscribeChannelClient(channel) => SubscribeMessage(channel, userSink, replyTo)
          }
      }

      val channelAnswerPartitioner = builder.add(Partition[ChannelActorAnswer](2, {
        _ match {
          case Answer(_) => 0
          case AnswerSubscribe(_, _, _) => 1
        }
      }))


      val answerToJSON = Flow[ChannelActorAnswer].map{ case Answer(jsonMessage) => jsonMessage}

      val unsubHandler = Flow[UnsubMessage].statefulMapConcat(() =>{
        var channels: Map[String, UniqueKillSwitch] = Map.empty

        { _ match {
           case AnswerSubscribe(jsonMessage, channel, Some(killSwitch)) =>
             channels = channels + (channel -> killSwitch)
            List(jsonMessage)
           case AnswerSubscribe(jsonMessage, _, None) =>
            List(jsonMessage)
           case UnsubRequest(channel) =>
            val message =
              if(channels.contains(channel)) {
              channels(channel).shutdown()
              channels = channels.removed(channel)
              AnswerMessageServer(true, None)
            }
            else {
              AnswerMessageServer(false, Some("You are not subscribed to this channel."))
            }
            List(message)
         }
         }
      })

      val unsubMap = Flow[JsonMessage].map{case UnsubscribeChannelClient(channel) => UnsubRequest(channel)}

      val unsubMap2 = Flow[ChannelActorAnswer].map{case a: AnswerSubscribe => a}

      val writeDb = ActorFlow.ask[JsonMessage, DBMessage, NotifyChannelServer](dbActor) {
        (message, replyTo) =>
          message match {
            case PublishChannelClient(channel, event) =>
              //TODO: compute key using hash of event once the specs are updated
              val id = event
              Write(channel, id, event, replyTo)
          }
      }

      val fetchDb = ActorFlow.ask[JsonMessage, DBMessage, JsonMessage](dbActor) {
        (message, replyTo) =>
          message match {
            case FetchChannelClient(channel, id) =>
              Read(channel, id, replyTo)
          }
      }

      //Connect the graph
      input ~> partitioner
      partitioner.out(Publish) ~> writeDb ~> pubEntry
      partitioner.out(Create) ~> actorFlow ~> channelAnswerPartitioner
      channelAnswerPartitioner.out(0) ~> answerToJSON ~> merge
      channelAnswerPartitioner.out(1) ~> unsubMap2 ~> mergeUnsub
      partitioner.out(Unsubscribe) ~> unsubMap ~> mergeUnsub
      mergeUnsub ~> unsubHandler ~> merge

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
  def messageFlow(pubEntry: Sink[NotifyChannelServer, NotUsed], actor: ActorRef[ChannelMessage], dbActor : ActorRef[DBMessage])(implicit timeout: Timeout, system: ActorSystem[Nothing]): Flow[Message, Message, NotUsed] = {
    val jFlow = jsonFlow(pubEntry, actor, dbActor)
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
