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

  // This function packs each message into a publish before pushing them into the pipeline
  def responseHandler(messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(JsonRpcResponse(_, Some(resultObject), None, _)) =>
      resultObject.resultMap match {
        case Some(resultMap) =>
          val receivedResponse = wrapMsgInPublish(resultMap)
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

  // This function will try to digest the set of messages for an entire channel
  // It will go channel by channel until it finished processing messages or MAX_RETRY_PER_MESSAGE * total_number_of_message is reached
  @tailrec
  private def passThroughPipeline(
      receivedResponse: List[(GraphMessage, Int)],
      validatorFlow: Flow[GraphMessage, GraphMessage, NotUsed]
  )(implicit system: ActorSystem): Boolean = {
    if (receivedResponse.isEmpty || receivedResponse.forall(elem => elem._2 <= 0)) {
      return receivedResponse.isEmpty
    }

    var failedMessages: List[(GraphMessage, Int)] = Nil
    receivedResponse.foreach {
      case (graphMessage, retry) =>
        if (retry > 0) {
          messagesThroughPipeline(validatorFlow, graphMessage) match {
            case Left(err) =>
              failedMessages = (graphMessage, retry - 1) :: failedMessages
              // only log a during last attempt
              if (retry == 1) {
                println("Errors: " + err.toString + "\n On:\n" + prettyPrinter(graphMessage))
              }
            case _ => /* DO NOTHING */
          }
        }
    }

    passThroughPipeline(failedMessages.reverse, validatorFlow)
  }

  // Push a message corresponding through the pipeline
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

  // Must return a list sorted by channel length
  private def wrapMsgInPublish(map: Map[Channel, Set[Message]]): List[(GraphMessage, Int)] = {
    var publishedList = map.foldLeft(List.empty[JsonRpcRequest])((acc, elem) =>
      acc ++ elem._2.map(msg =>
        JsonRpcRequest(
          RpcValidator.JSON_RPC_VERSION,
          MethodType.PUBLISH,
          new Publish(elem._1, msg),
          Some(0)
        )
      )
    )
    publishedList = publishedList.sortWith((e1, e2) => e1.getParamsChannel.channel.length <= e2.getParamsChannel.channel.length)
    publishedList.map(js => (Right(js), MAX_RETRY_PER_MESSAGE))
  }
}
