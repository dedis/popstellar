package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.model.network.method.{Catchup, Subscribe, Unsubscribe}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ParamsWithChannelHandler extends AskPatternConstants {

  def graph(clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        /* partitioner port numbers */
        val portPipelineError = 0
        val portSubscribe = 1
        val portUnsubscribe = 2
        val portCatchup = 3
        val totalPorts = 4

        /* building blocks */
        val handlerPartitioner = builder.add(Partition[GraphMessage](
          totalPorts,
          {
            case Right(jsonRpcMessage: JsonRpcRequest) => jsonRpcMessage.getParams match {
                case _: Subscribe   => portSubscribe
                case _: Unsubscribe => portUnsubscribe
                case _: Catchup     => portCatchup
              }
            case _ => portPipelineError // Pipeline error goes directly in handlerMerger
          }
        ))

        val subscribeHandler = builder.add(ParamsWithChannelHandler.subscribeHandler(clientActorRef))
        val unsubscribeHandler = builder.add(ParamsWithChannelHandler.unsubscribeHandler(clientActorRef))
        val catchupHandler = builder.add(ParamsWithChannelHandler.catchupHandler(clientActorRef))

        val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

        /* glue the components together */
        handlerPartitioner.out(portPipelineError) ~> handlerMerger
        handlerPartitioner.out(portSubscribe) ~> subscribeHandler ~> handlerMerger
        handlerPartitioner.out(portUnsubscribe) ~> unsubscribeHandler ~> handlerMerger
        handlerPartitioner.out(portCatchup) ~> catchupHandler ~> handlerMerger

        /* close the shape */
        FlowShape(handlerPartitioner.in, handlerMerger.out)
      }
  })

  final case class Asking(g: GraphMessage, replyTo: ActorRef)

  def subscribeHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val channel: Channel = jsonRpcMessage.getParams.channel
      val ask: Future[GraphMessage] = (clientActorRef ? ClientActor.SubscribeTo(jsonRpcMessage.getParams.channel)).map {
        case PubSubMediator.SubscribeToAck(returnedChannel) if returnedChannel == channel =>
          Right(jsonRpcMessage)
        case PubSubMediator.SubscribeToAck(returnedChannel) =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"PubSubMediator subscribed client to channel '$returnedChannel' instead of '$channel'", jsonRpcMessage.id))
        case PubSubMediator.SubscribeToNAck(returnedChannel, reason) if returnedChannel == channel =>
          Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"Could not subscribe client to channel '$returnedChannel': $reason", jsonRpcMessage.id))
        case PubSubMediator.SubscribeToNAck(returnedChannel, reason) => Left(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"PubSubMediator tried to subscribe client to channel '$returnedChannel' instead of '$channel' but could not: $reason",
            jsonRpcMessage.id
          ))
        case _ =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "Client actor returned an unknown answer", jsonRpcMessage.id))
      }

      Await.result(ask, duration)

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "SubscribeHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }

  def unsubscribeHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val channel: Channel = jsonRpcMessage.getParams.channel
      val ask: Future[GraphMessage] = (clientActorRef ? ClientActor.UnsubscribeFrom(channel)).map {
        case PubSubMediator.UnsubscribeFromAck(returnedChannel) if returnedChannel == channel =>
          Right(jsonRpcMessage)
        case PubSubMediator.UnsubscribeFromAck(returnedChannel) =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"PubSubMediator unsubscribe client from channel '$returnedChannel' instead of '$channel'", jsonRpcMessage.id))
        case PubSubMediator.UnsubscribeFromNAck(returnedChannel, reason) if returnedChannel == channel =>
          Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"Could not unsubscribe client from channel '$returnedChannel': $reason", jsonRpcMessage.id))
        case PubSubMediator.UnsubscribeFromNAck(returnedChannel, reason) => Left(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"PubSubMediator tried to unsubscribe client from channel '$returnedChannel' instead of '$channel' but could not: $reason",
            jsonRpcMessage.id
          ))
        case _ =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "Client actor returned an unknown answer", jsonRpcMessage.id))
      }

      Await.result(ask, duration)

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "UnsubscribeHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }

  // Catchup requests are treated at the AnswerGenerator stage since it generates a JsonRpcResponse directly
  def catchupHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(m => m)
}
