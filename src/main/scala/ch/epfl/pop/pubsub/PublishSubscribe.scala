package ch.epfl.pop.pubsub

import java.util.Base64
import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Merge, MergeHub, Partition, Sink, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.stream.{FlowShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.Timeout
import ch.epfl.pop.{DBActor, Validate}
import ch.epfl.pop.DBActor.{DBMessage, Read, Write}
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages.{JsonMessagePublishClient, _}
import ch.epfl.pop.json.JsonUtils.ErrorCodes.InvalidData
import ch.epfl.pop.json.JsonUtils.{ErrorCodes, JsonMessageParserError}
import ch.epfl.pop.json.{MessageContent, MessageErrorContent, MessageParameters}
import ch.epfl.pop.pubsub.ChannelActor._

import java.util
import scala.concurrent.{Await, Future}



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
               system: ActorSystem[Nothing], pubEntry: Sink[PropagateMessageServer, NotUsed]): Flow[JsonMessagePubSubClient, JsonMessageAnswerServer, NotUsed] = {

    val (userSink, userSource) = MergeHub.source[PropagateMessageServer].toMat(BroadcastHub.sink)(Keep.both).run()

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //Initialize Inlet/Outlets
      val input = builder.add(Flow[JsonMessagePubSubClient])
      val merge = builder.add(Merge[JsonMessageAnswerServer](4))
      val output = builder.add(Flow[JsonMessageAnswerServer])

      val Subscribe = 0
      val Unsubscribe = 3
      val Publish = 1
      val Catchup = 2
      val partitioner = builder.add(Partition[JsonMessagePubSubClient](4,
        {
          _ match {
            case _ : SubscribeMessageClient => Subscribe
            case _ : UnsubscribeMessageClient => Unsubscribe
            case m : JsonMessagePublishClient => Publish
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
                      system: ActorSystem[Nothing], pubEntry: Sink[PropagateMessageServer, NotUsed]): JsonMessage => JsonMessageAnswerServer = {

    def pub(params: MessageParameters, propagate: Boolean) = {
      system.log.debug("Publishing: " + util.Arrays.toString(params.message.get.message_id))
      val future = dbActor.ask(ref => Write(params.channel, params.message.get, ref))
      Await.result(future, timeout.duration)
      if(propagate) {
        val prop = PropagateMessageServer(params)
        Source.single(prop).runWith(pubEntry)
      }
    }

    def errorOrPublish(params: MessageParameters, id: Int, error: Option[MessageErrorContent]) = {
      error match {
        case Some(error) => AnswerErrorMessageServer(id, error)
        case None =>
          val content = params.message.get
          Validate.validate(content) match {
            case Some(error) => AnswerErrorMessageServer(id, error)
            case None =>
              pub(params, true)
              AnswerResultIntMessageServer(id)
          }
      }
    }

    def pubCreateLao(m: CreateLaoMessageClient) = {
      val params = m.params
      val id = m.id
      Validate.validate(m) match {
        case Some(error) => AnswerErrorMessageServer(id, error)
        case None =>
          val highLevelMessage = params.message.get.data
          val channel = "/root/" + new String(Base64.getEncoder.encode(highLevelMessage.id))
          val future = actor.ask(ref => CreateMessage(channel, ref))

          if (!Await.result(future, timeout.duration)) {
            val error = MessageErrorContent(-3, "Channel " + channel + " already exists.")
            AnswerErrorMessageServer(error = error, id = id)
          }
          else {
            //Publish on the LAO main channel
            pub(MessageParameters(channel, params.message), false)
            AnswerResultIntMessageServer(id)
          }
      }
    }

    def pubWitness(m: WitnessMessageMessageClient) = {
      val params = m.params
      val id = m.id
      val message = params.message.get.data
      val messageId = message.message_id
      val signature = message.signature
      implicit val ec = system.executionContext
      val future = dbActor.ask(ref => Read(params.channel, Base64.getDecoder.decode(messageId), ref))
        .flatMap {
          case Some(message) =>
            dbActor.ask(ref => Write(params.channel, message.updateWitnesses(signature), ref))
          case None =>
            system.log.debug("Message id not found in db.")
            Future(false)
        }
      if (Await.result(future, timeout.duration))
        errorOrPublish(params, id, Validate.validate(m))
      else {
        val error = MessageErrorContent(ErrorCodes.InvalidData.id, "The id the witness message refers to does not exist.")
        AnswerErrorMessageServer(id, error)
      }
    }

    _ match {
        case m @ CreateLaoMessageClient(params, id, _, _) =>
          pubCreateLao(m)
        case m @ UpdateLaoMessageClient(params,id,_,_) =>
          errorOrPublish(params, id, Validate.validate(m))
        case m @ BroadcastLaoMessageClient(params, id,_,_) =>
          val future = dbActor.ask(ref => Read(params.channel, params.message.get.data.modification_id, ref))
          Await.result(future, timeout.duration) match {
            case None =>
             // system.log.debug("Reading: " + params.message.get.data.modification_id)
              AnswerErrorMessageServer(id, MessageErrorContent(InvalidData.id, "Invalid reference to a message_id"))
            case Some(msgContent) =>
              errorOrPublish(params, id, Validate.validate(m, msgContent.data))
          }
        case m @ WitnessMessageMessageClient(_, _, _, _) =>
          pubWitness(m)
        case m @ CreateMeetingMessageClient(params, id, _, _) =>
          val laoId = Base64.getDecoder.decode(params.channel.slice(6,params.channel.length).getBytes)
          errorOrPublish(params, id, Validate.validate(m, laoId))
        case m @ BroadcastMeetingMessageClient(params, id, _ ,_) =>
          val future = dbActor.ask(ref => Read(params.channel, params.message.get.data.modification_id, ref))
          Await.result(future, timeout.duration) match {
            case None => AnswerErrorMessageServer(id, MessageErrorContent(InvalidData.id, "Invalid reference to a message_id"))
            case Some(msgContent) =>
              errorOrPublish(params, id, Validate.validate(m, msgContent.data))
          }
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
                  system: ActorSystem[Nothing], pubEntry: Sink[PropagateMessageServer, NotUsed]): Flow[Message, Message, NotUsed] = {
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
          case TextMessage.Strict(s) => parseMessage(s) match {
            case Left(m) => m
            case Right(JsonMessageParserError(description, id, errorCode)) =>
              AnswerErrorMessageServer(id, MessageErrorContent(errorCode.id, description))
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
