package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.MethodType.GREET_SERVER
import ch.epfl.pop.model.network.method.GreetServer
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ParamsHandler extends AskPatternConstants {

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

  def greetServerHandler(clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      jsonRpcMessage.method match {
        case GREET_SERVER =>
          val greetServer: GreetServer = jsonRpcMessage.getParams.asInstanceOf[GreetServer]
          clientActorRef ! greetServer
          Right(jsonRpcMessage)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received a non expected jsonRpcRequest", jsonRpcMessage.id))
      }
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received an unexpected message:" + graphMessage, None))
  }

}
