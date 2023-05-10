package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.epfl.pop.model.network.method.Publish
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError, prettyPrinter}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PublishSubscribe}

import scala.annotation.tailrec
import scala.concurrent.Await

//This object's job is to handle responses it receives from other servers after sending a heartbeat.
// When receiving the missing messages, the server's job is to write them on the database.
object GetMessagesByIdResponseHandler extends AskPatternConstants {

  private final val MAX_RETRY = 5
  private final val SUCCESS = 0

  // This function packs each message into a publish before pushing them into the pipeline
  def responseHandler(
      mediatorActorRef: ActorRef,
      messageRegistry: MessageRegistry
  )(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(JsonRpcResponse(_, Some(resultObject), None, _)) =>
      resultObject.resultMap match {
        case Some(resultMap) =>
          val receivedResponse: Map[Channel, Set[GraphMessage]] = wrapMsgInPublish(resultMap)
          val success: Boolean = passThroughPipeline(receivedResponse, PublishSubscribe.validateRequests(mediatorActorRef, messageRegistry), MAX_RETRY)
          if (success) {
            Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(SUCCESS), None))
          } else {
            Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GetMessagesById handler failed to process some messages", None))
          }

        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GetMessagesById handler received an unexpected response message", None))
      }

    // Will end up in a Sink.ignore, safe to let through
    case value @ _ => value
  }

  // This function will try to digest the set of messages for an entire channel
  // It will go channel by channel until it finished processing messages or MAX_RETRY is reached
  @tailrec
  private def passThroughPipeline(
      receivedResponse: Map[Channel, Set[GraphMessage]],
      validatorFlow: Flow[GraphMessage, GraphMessage, NotUsed],
      remainingAttempts: Int
  )(implicit system: ActorSystem): Boolean = {
    if (receivedResponse.isEmpty || remainingAttempts <= 0) {
      return receivedResponse.isEmpty
    }

    var failedMessages: Map[Channel, Set[GraphMessage]] = Map.empty
    receivedResponse.foreach {
      case (channel, messagesSet) =>
        val failedMessageMap = messagesThroughPipeline(validatorFlow, messagesSet)
        if (failedMessageMap.nonEmpty) {
          failedMessages += channel -> failedMessageMap.keySet
          // only log a during last attempt
          if (remainingAttempts == 1) {
            println("Errors: " + failedMessageMap.map {
              case (msg, error) => error.toString + "\n On:\n" + prettyPrinter(msg)
            })
          }
        }
    }

    passThroughPipeline(failedMessages, validatorFlow, remainingAttempts - 1)
  }

  // Push a set of message corresponding to one channel through the pipeline
  private def messagesThroughPipeline(
      validator: Flow[GraphMessage, GraphMessage, NotUsed],
      messageSet: Set[GraphMessage]
  )(implicit system: ActorSystem): Map[GraphMessage, PipelineError] = {
    var failedGraphMessages: Map[GraphMessage, PipelineError] = Map.empty
    messageSet.foreach { message =>
      val singleRun = Source.single(message).via(validator).runWith(Sink.foreach {
        case Left(err: PipelineError) =>
          failedGraphMessages += message -> err

        case _ => /* Nothing to do */
      })
      Await.ready(singleRun, duration)
    }

    failedGraphMessages
  }

  private def wrapMsgInPublish(map: Map[Channel, Set[Message]]): Map[Channel, Set[GraphMessage]] = {
    map.map {
      case (channel, set) =>
        channel -> set.map(msg =>
          Right(
            JsonRpcRequest(
              RpcValidator.JSON_RPC_VERSION,
              MethodType.PUBLISH,
              new Publish(channel, msg),
              Some(0)
            )
          )
        )
    }
  }
}
