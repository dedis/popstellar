package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition}
import akka.stream.typed.scaladsl.ActorFlow
import akka.stream.{FlowShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.Timeout
import ch.epfl.pop.DBActor
import ch.epfl.pop.DBActor.{DBMessage, Write}
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages.{JsonMessageAdminClient, _}
import ch.epfl.pop.json.{MessageErrorContent, MessageParameters}
import ch.epfl.pop.pubsub.ChannelActor._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


object PublishSubscribe {
  /**
   * Create a flow that handles JSON messages of a publish-subscribe system.
   *
   * @param channelActor    an actor handling channel creation and subscription
   * @return a flow that handles JSON messages of a publish-subscribe system
   */
  def jsonFlow(channelActor: ActorRef[ChannelMessage],
               dbActor : ActorRef[DBMessage])
              (implicit timeout: Timeout,
               system: ActorSystem[Nothing]): Flow[JsonMessagePubSubClient, JsonMessageAnswerServer, NotUsed] = {

    val (userSink, userSource) = MergeHub.source[PropagateMessageServer].toMat(BroadcastHub.sink)(Keep.both).run()

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //Initialize Inlet/Outlets
      val input = builder.add(Flow[JsonMessagePubSubClient])
      val merge = builder.add(Merge[JsonMessageAnswerServer](5))
      val output = builder.add(Flow[JsonMessageAnswerServer])
      //val mergeUnsub = builder.add(Merge[UnsubMessage](2))

      val Subscribe = 0
      val Unsubscribe = 3
      val Publish = 1
      val Catchup = 2
      val partitioner = builder.add(Partition[JsonMessagePubSubClient](4,
        {
          _ match {
            case _ : SubscribeMessageClient => Subscribe
            case _ : UnsubscribeMessageClient => Unsubscribe
            case _ : JsonMessageAdminClient => Publish // TODO un JsonMessagePubSub client ne peut jamais être également un JsonMessageAdminClient :thinking:
            case _ : CatchupMessageClient => Catchup
          }
        }
      ))

      //ActorFlow handling channel subscription
      val actorFlow = ActorFlow.ask[JsonMessage, ChannelMessage, ChannelActorAnswer](channelActor) {
        (message, replyTo) =>
          message match {
            case SubscribeMessageClient(params, id, _, _) => SubscribeMessage(params.channel, userSink, id, replyTo)
          }
      }

      val unsub = builder.add(unsubHandler())
      val unsubMap = Flow[JsonMessage].map{case UnsubscribeMessageClient(params, id, _, _) => UnsubRequest(params.channel, id)}
      val unsubMap2 = Flow[ChannelActorAnswer].map{case a: AnswerSubscribe => a}
      val publishFlow = Flow[JsonMessage].map(publish(channelActor, dbActor))

      val catchupDB = ActorFlow.ask[JsonMessagePubSubClient, DBMessage, JsonMessageAnswerServer](dbActor) {
        (message, replyTo) =>
          message match {
            case CatchupMessageClient(params, id, _, _) =>
              DBActor.Catchup(params.channel, id, replyTo)
          }
      }

      //Connect the graph
      input ~> partitioner
      partitioner.out(Publish) ~> publishFlow ~> merge
      partitioner.out(Subscribe) ~> actorFlow ~> unsubMap2 ~> unsub
      partitioner.out(Unsubscribe) ~> unsubMap ~> unsub ~> merge
      partitioner.out(Catchup) ~> catchupDB ~> merge
      userSource ~> merge
      merge ~> output

      FlowShape(input.in, output.out)
    }
    )
  }

  private def publish(actor: ActorRef[ChannelMessage],
                      dbActor: ActorRef[DBMessage])
                     (implicit timeout: Timeout,
                      system: ActorSystem[Nothing]): JsonMessage => JsonMessageAnswerServer = {

    def pub(params: MessageParameters, id: Int) = {
      val future = dbActor.ask(ref => Write(params, id, ref))
      Await.result(future, timeout.duration)
    }

    m =>
      m match {
        case CreateLaoMessageClient(_, _, params, id) =>
          val highLevelMessage = params.message.get.data
          val channel = "root/" + highLevelMessage.id
          val future = actor.ask(ref => CreateMessage(channel, ref))
          if (!Await.result(future, timeout.duration)) {
            val error = MessageErrorContent(-3, "Channel " + channel + " already exists.")
            AnswerErrorMessageServer(error = error, id = id)
          }
          else {
            pub(params, id)
          }
        case req: JsonMessageAdminClient =>
          pub(req.params, req.id)
      }
  }

  private def unsubHandler() = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._
    val merge = builder.add(Merge[UnsubMessage](2))

    val unsub = Flow[UnsubMessage].statefulMapConcat{() =>
      var channels: Map[String, UniqueKillSwitch] = Map.empty

      {
        case AnswerSubscribe(jsonMessage, channel, Some(killSwitch)) =>
          channels = channels + (channel -> killSwitch)
          List(jsonMessage)

        case AnswerSubscribe(jsonMessage, _, None) =>
          List(jsonMessage)

        case UnsubRequest(channel, id) =>

          val message =
            if (channels.contains(channel)) {
              channels(channel).shutdown()
              channels = channels.removed(channel)
              AnswerResultIntMessageServer(id = id)
            }
            else {
              val error = MessageErrorContent(-2, "Invalid resource: you are not subscribed to channel " + channel + ".")
              AnswerErrorMessageServer(error = error, id = id)
            }
          List(message)
      }
    }

    val out = merge.out ~> unsub

    UniformFanInShape(out.outlet, merge.in(0), merge.in(1))
  }

  /**
   * Create a flow that handles messages of a publish-subscribe system.
   *
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles messages of a publish-subscribe system
   */
  def messageFlow(actor: ActorRef[ChannelMessage],
                  dbActor : ActorRef[DBMessage])
                 (implicit timeout: Timeout,
                  system: ActorSystem[Nothing]): Flow[Message, Message, NotUsed] = {
    val jFlow = jsonFlow(actor, dbActor)
    Flow.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        val input = builder.add(Flow[Message])
        val output = builder.add(Flow[Message])
        val partitioner = builder.add(Partition[JsonMessage]( 2, {
          case _ :JsonMessageAnswerServer => 0
          case _ => 1
        }))
        val merge = builder.add(Merge[JsonMessage](2))


        val parser = Flow[Message].map {
          case TextMessage.Strict(s) => Try(parseMessage(s)) match {
            case Success(message) => message
            case Failure(exception) =>
              val error = MessageErrorContent(-4, "Invalid JSON.")
              AnswerErrorMessageServer(error = error, id = 0) //TODO: find a better way than using a fixed id
          }
        }
        val formatter = Flow[JsonMessage].map(m => TextMessage.Strict(serializeMessage(m)))

        val mapPubSub = Flow[JsonMessage].map{case m: JsonMessagePubSubClient => m}

        input ~> parser ~> partitioner
        partitioner.out(0) ~> merge
        partitioner.out(1) ~> mapPubSub ~> jFlow ~> merge
        merge ~> formatter ~> output

        FlowShape(input.in, output.out)
    })
  }
}
