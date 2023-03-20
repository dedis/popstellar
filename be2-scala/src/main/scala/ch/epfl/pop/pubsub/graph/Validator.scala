package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method._
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.MessageRegistry
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.validators.ParamsValidator._
import ch.epfl.pop.pubsub.graph.validators.RpcValidator._

object Validator {

  private def validationError(rpcId: Option[Int]): PipelineError = PipelineError(
    ErrorCodes.INVALID_ACTION.id,
    "Unsupported action: Validator was given a message it could not recognize",
    rpcId
  )

  private def validateJsonRpcContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Right(jsonRpcMessage) => jsonRpcMessage match {
        case message @ (_: JsonRpcRequest)  => validateRpcRequest(message)
        case message @ (_: JsonRpcResponse) => validateRpcResponse(message)
        case _                              => Left(validationError(None)) // should never happen
      }
    case graphMessage @ _ => graphMessage
  }

  private def validateMethodContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Right(jsonRpcRequest: JsonRpcRequest) => jsonRpcRequest.getParams match {
        case _: Broadcast   => validateBroadcast(jsonRpcRequest)
        case _: Catchup     => validateCatchup(jsonRpcRequest)
        case _: Publish     => validatePublish(jsonRpcRequest)
        case _: Subscribe   => validateSubscribe(jsonRpcRequest)
        case _: Unsubscribe => validateUnsubscribe(jsonRpcRequest)
        case _              => Left(validationError(jsonRpcRequest.id))
      }
    case Right(jsonRpcResponse: JsonRpcResponse) => Left(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Unsupported action: MethodValidator was given a response message",
        jsonRpcResponse.id
      ))
    case _ => graphMessage
  }

  private def validateMessageContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Right(jsonRpcRequest: JsonRpcRequest) => jsonRpcRequest.getParams match {
        case _: Broadcast   => validateMessage(jsonRpcRequest)
        case _: Catchup     => graphMessage
        case _: Publish     => validateMessage(jsonRpcRequest)
        case _: Subscribe   => graphMessage
        case _: Unsubscribe => graphMessage
        case _              => Left(validationError(jsonRpcRequest.id))
      }
    case graphMessage @ _ => graphMessage
  }

  def validateHighLevelMessage(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Right(_) => validateJsonRpcContent(graphMessage) match {
        case Right(_) => validateMethodContent(graphMessage) match {
            case Right(_) => validateMessageContent(graphMessage) match {
                case Right(_)          => graphMessage
                case graphMessage @ _ => graphMessage
              }
            case graphMessage @ _ => graphMessage
          }
        case graphMessage @ _ => graphMessage
      }
    case graphMessage @ _ => graphMessage
  }

  def validateMessageDataContent(rpcRequest: JsonRpcRequest, registry: MessageRegistry): GraphMessage = {
    val (_object, action) = rpcRequest.getDecodedDataHeader
    registry.getValidator(_object, action) match {
      case Some(validator) => validator(rpcRequest)
      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"MessageRegistry could not find any data validator for JsonRpcRequest : $rpcRequest'",
          rpcRequest.getId
        ))
    }
  }

  // takes a JsonRpcMessage and validates input until Message layer
  val jsonRpcContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateHighLevelMessage)

  // validation from Message layer
  def messageContentValidator(messageRegistry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(rpcRequest: JsonRpcRequest) => validateMessageDataContent(rpcRequest, messageRegistry)
    case Right(rpcResponse: JsonRpcResponse) => Left(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "'messageContentValidator' was called on a JsonRpcResponse, which by definition, does not contain any Message layer'",
        rpcResponse.getId
      ))
    case graphMessage @ _ => graphMessage
  }
}
