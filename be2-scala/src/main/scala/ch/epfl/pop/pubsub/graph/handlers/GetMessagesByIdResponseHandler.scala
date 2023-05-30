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
import scala.util.Success
import scala.concurrent.Await

//This object's job is to handle responses it receives from other servers after sending a heartbeat.
// When receiving the missing messages, the server's job is to write them on the database.
object GetMessagesByIdResponseHandler extends AskPatternConstants {

  private val MAX_RETRY_PER_MESSAGE = 5
  private val SUCCESS = 0

  /** Validate and replay on the system each message received in the get_messages_by_id result
    *
    * @param messageRegistry
    *   The system message registry to use in the validation pipeline
    * @param system
    *   Implicit actor system to use the given validator
    * @return
    *   Left if some messages couldn't be validated after MAX_RETRY_PER_MESSAGE times, Right for success
    */
  def responseHandler(messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(JsonRpcResponse(_, Some(resultObject), None, _)) =>
      resultObject.resultMap match {
        case Some(resultMap) =>
          val receivedResponse = wrapMsgInPublish(resultMap, MAX_RETRY_PER_MESSAGE)
          val validator = PublishSubscribe.validateRequests(ActorRef.noSender, messageRegistry)
          val success: Boolean = passThroughPipeline(receivedResponse, validator)
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

  /** Will try to digest each GraphMessage until their retry-counter reaches 0 or they all get validated
    *
    * @param receivedResponse
    *   The wrapped messages with their counter
    * @param validatorFlow
    *   The validator to push the messages through
    * @param logListFailure
    *   The list of accumulated error logs, default to Nil
    * @param system
    *   Implicit actor system to use the given validator
    * @return
    *   true if all messages could be validated, otherwise false
    */
  @tailrec
  private def passThroughPipeline(
      receivedResponse: List[(GraphMessage, Int)],
      validatorFlow: Flow[GraphMessage, GraphMessage, NotUsed],
      logListFailure: List[String] = Nil
  )(implicit system: ActorSystem): Boolean = {
    if (receivedResponse.isEmpty) {
      if (logListFailure.nonEmpty) {
        logListFailure.foreach(log => println(log))
      }
      return logListFailure.isEmpty
    }

    var failedMessages: List[(GraphMessage, Int)] = Nil
    var logs: List[String] = logListFailure
    receivedResponse.foreach {
      case (graphMessage, retry) =>
        if (retry > 0) {
          messagesThroughPipeline(validatorFlow, graphMessage) match {
            case Left(err) =>
              if (retry == 1) {
                // only log a during last attempt
                logs ::= "Errors: " + err.toString + "\n On:\n" + prettyPrinter(graphMessage)
              } else {
                failedMessages ::= graphMessage -> (retry - 1)
              }

            case _ => /* DO NOTHING */
          }
        }
    }

    passThroughPipeline(failedMessages, validatorFlow, logs)
  }

  /** Push a message through the given validator
    *
    * @param validator
    *   The pipeline verifying the message
    * @param message
    *   The message to verify
    * @param system
    *   Implicit actor system to use the given validator
    * @return
    *   The result of the validator on the given message, Left for failure, Right for success
    */
  private def messagesThroughPipeline(
      validator: Flow[GraphMessage, GraphMessage, NotUsed],
      message: GraphMessage
  )(implicit system: ActorSystem): GraphMessage = {
    val output = Source.single(message).via(validator).runWith(Sink.head)
    Await.ready(output, duration).value.get match {
      case Success(res @ _) => res
      case err @ _          => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, err.toString, None))
    }
  }

  /** Wrap messages in Publish RPCs along with a counter
    *
    * @param map
    *   A map of channels to their set of messages
    * @return
    *   A list of GraphMessage - Int pairs where the GraphMessage are sorted in increasing order of their channel length and the Int is the number of retry left for each message
    */
  private def wrapMsgInPublish(map: Map[Channel, Set[Message]], retryPerMessage: Int): List[(GraphMessage, Int)] = {
    val publishedList: List[JsonRpcRequest] = map.flatMap {
      case (channel, set) =>
        set.map(msg =>
          JsonRpcRequest(
            RpcValidator.JSON_RPC_VERSION,
            MethodType.PUBLISH,
            new Publish(channel, msg),
            Some(0)
          )
        )
    }.toList

    publishedList
      .sortWith((jsonMsg1, jsonMsg2) => jsonMsg1.getParamsChannel.channel.length <= jsonMsg2.getParamsChannel.channel.length)
      .map(js => (Right(js), retryPerMessage))
  }
}
