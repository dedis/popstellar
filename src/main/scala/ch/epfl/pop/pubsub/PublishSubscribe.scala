package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition}
import akka.stream.typed.scaladsl.ActorFlow
import akka.stream.{FlowShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.Timeout
import ch.epfl.pop.DBActor
import ch.epfl.pop.DBActor.{DBMessage, Write}
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.json.MessageErrorContent
import ch.epfl.pop.pubsub.ChannelActor._

import scala.util.{Failure, Success, Try}


object PublishSubscribe {
  /**
   * Create a flow that handles JSON messages of a publish-subscribe system.
   *
   * @param actor    an actor handling channel creation and subscription
   * @return a flow that handles JSON messages of a publish-subscribe system
   */
  def jsonFlow(actor: ActorRef[ChannelMessage],
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

      val Create = 0
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
            //case CreateChannelClient(_, _) => Create
          }
        }
      ))

      //ActorFlow handling channel creation and subscription
      val actorFlow = ActorFlow.ask[JsonMessage, ChannelMessage, ChannelActorAnswer](actor) {
        (message, replyTo) =>
          message match {
            //case CreateChannelClient(message, contract) => CreateMessage(message, replyTo)
            //case SubscribeChannelClient(channel) => SubscribeMessage(channel, userSink, replyTo)
            case SubscribeMessageClient(params, id, _, _) => SubscribeMessage(params.channel, userSink, id, replyTo)

          }
      }

      val channelAnswerPartitioner = builder.add(Partition[ChannelActorAnswer](2, {
        case AnswerCreate(_) => 0
        case AnswerSubscribe(_, _, _) => 1
      }))


      val answerToJSON = Flow[ChannelActorAnswer].map{ case AnswerCreate(jsonMessage) => jsonMessage}

      val unsub = builder.add(unsubHandler())

      val unsubMap = Flow[JsonMessage].map{case UnsubscribeMessageClient(params, id, _, _) => UnsubRequest(params.channel, id)}

      val unsubMap2 = Flow[ChannelActorAnswer].map{case a: AnswerSubscribe => a}

      val writeDb = ActorFlow.ask[JsonMessage, DBMessage, JsonMessageAnswerServer](dbActor) {
        (message, replyTo) =>
          message match {
            case req : JsonMessageAdminClient =>
              //TODO: compute key using hash of event once the specs are updated
              val m = req.params.message.get
              val messageData = ??? //TODO: get message data once it will be available as the part of the message
              Write(req.params.channel, m.message_id, messageData, req.id, replyTo)
          }
      }

      val catchupDB = ActorFlow.ask[JsonMessagePubSubClient, DBMessage, JsonMessageAnswerServer](dbActor) {
        (message, replyTo) =>
          message match {
            case CatchupMessageClient(params, id, _, _) =>
              DBActor.Catchup(params.channel, id, replyTo)
          }
      }

      //Connect the graph
      input ~> partitioner
      partitioner.out(Publish) ~> writeDb ~> merge
      partitioner.out(Create) ~> actorFlow ~> channelAnswerPartitioner
      channelAnswerPartitioner.out(0) ~> answerToJSON ~> merge
      channelAnswerPartitioner.out(1) ~> unsubMap2 ~> unsub
      partitioner.out(Unsubscribe) ~> unsubMap ~> unsub
      unsub ~> merge
      partitioner.out(Catchup) ~> catchupDB ~> merge
      //userSource ~> merge //TODO doesn't compile
      merge ~> output

      FlowShape(input.in, output.out)
    }
    )
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
