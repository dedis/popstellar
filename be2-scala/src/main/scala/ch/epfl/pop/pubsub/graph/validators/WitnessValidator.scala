package ch.epfl.pop.pubsub.graph.validators

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

case object WitnessValidator extends ContentValidator {
  /**
   * Validates a GraphMessage containing a witness rpc-message or a pipeline error
   */
  override val validator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestWitnessMessage) => validateWitnessMessage(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_FAULT.id,
        "Internal server fault: WitnessValidator was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }

  sealed def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "WitnessMessage")

    rpcMessage.getParamsMessage match {
      case Some(_) => Left(rpcMessage)
      case _ => Right(validationErrorNoMessage)
    }
  }
}
